/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.reha.impl.client.blocking;

import static io.wcm.caravan.reha.api.relations.StandardRelations.ALTERNATE;
import static io.wcm.caravan.reha.api.relations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.client.HalApiClient;
import io.wcm.caravan.reha.api.client.JsonResourceLoader;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.impl.client.ErrorHandlingTest;
import io.wcm.caravan.reha.impl.client.blocking.ResourceStateTest.ResourceWithRequiredState;
import io.wcm.caravan.reha.testing.resources.TestResource;
import io.wcm.caravan.reha.testing.resources.TestResourceState;
import io.wcm.caravan.reha.testing.resources.TestResourceTree;

/**
 * Variation of the tests in {@link io.wcm.caravan.reha.impl.client.RelatedResourceTest}
 * for blocking HAL API interfaces (i.e. that are not using reactive return types for their methods)
 */
public class RelatedResourceTest {

  private RequestMetricsCollector metrics;
  private JsonResourceLoader jsonLoader;
  private TestResource entryPoint;

  @BeforeEach
  public void setUp() {
    metrics = RequestMetricsCollector.create();

    TestResourceTree testResourceTree = new TestResourceTree();
    jsonLoader = testResourceTree;
    entryPoint = testResourceTree.getEntryPoint();
  }

  private <T> T createClientProxy(Class<T> halApiInterface) {
    HalApiClient client = HalApiClient.create(jsonLoader, metrics);
    T clientProxy = client.getEntryPoint(entryPoint.getUrl(), halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }


  @HalApiInterface
  interface ResourceWithSingleRelated {

    @RelatedResource(relation = ITEM)
    ResourceWithRequiredState getItem();
  }

  @Test
  public void required_linked_resource_should_be_emitted() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    TestResourceState linkedState = createClientProxy(ResourceWithSingleRelated.class)
        .getItem()
        .getProperties();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void required_linked_resource_should_fail_if_link_is_not_present() throws Exception {

    entryPoint.createLinked(ALTERNATE).setText("item text");

    Throwable ex = catchThrowable(
        () -> createClientProxy(ResourceWithSingleRelated.class).getItem());

    assertThat(ex).isInstanceOf(NoSuchElementException.class).hasMessageStartingWith("The invocation of ResourceWithSingleRelated#getItem() has failed");

  }

  @Test
  public void required_linked_resource_should_cause_HalApiClientException_if_missing() throws Exception {

    entryPoint.createLinked(ITEM).withStatus(404);

    ErrorHandlingTest.assertHalApiClientExceptionIsThrownWithStatus(404,
        () -> createClientProxy(ResourceWithSingleRelated.class).getItem().getProperties());
  }

  @Test
  public void required_embedded_resource_should_be_emitted() throws Exception {

    entryPoint.createEmbedded(ITEM).setText("item text");

    TestResourceState linkedState = createClientProxy(ResourceWithSingleRelated.class)
        .getItem()
        .getProperties();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void required_embedded_resource_should_fail_if_resource_is_not_present() throws Exception {

    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    Throwable ex = catchThrowable(
        () -> createClientProxy(ResourceWithSingleRelated.class).getItem().getProperties());

    assertThat(ex).isInstanceOf(NoSuchElementException.class).hasMessageStartingWith("The invocation of ResourceWithSingleRelated#getItem() has failed");
  }


  @HalApiInterface
  interface ResourceWithOptionalRelated {

    @RelatedResource(relation = ITEM)
    Optional<ResourceWithRequiredState> getOptionalItem();
  }

  @Test
  public void optional_linked_resource_should_be_emitted() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    TestResourceState linkedState = createClientProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem()
        .map(ResourceWithRequiredState::getProperties)
        .get();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void optional_linked_resource_should_be_empty_if_link_is_not_present() throws Exception {

    // create a link with a different relation then defined in the interface
    entryPoint.createLinked(ALTERNATE).setText("item text");

    Optional<ResourceWithRequiredState> maybeLinked = createClientProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem();

    assertThat(maybeLinked.isPresent()).isFalse();
  }

  @Test
  public void optional_embedded_resource_should_be_emitted() throws Exception {

    entryPoint.createEmbedded(ITEM).setText("item text");

    TestResourceState linkedState = createClientProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem()
        .map(ResourceWithRequiredState::getProperties)
        .get();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void optional_embedded_resource_should_be_empty_if_resource_is_not_present() throws Exception {

    // create a link with a different relation then defined in the interface
    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    Optional<ResourceWithRequiredState> maybeEmbedded = createClientProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem();

    assertThat(maybeEmbedded.isPresent()).isFalse();
  }


  @HalApiInterface
  interface ResourceWithMultipleRelated {

    @RelatedResource(relation = ITEM)
    List<ResourceWithRequiredState> getItems();
  }

  @Test
  public void list_linked_resource_should_emitted_single_item() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    TestResourceState linkedState = createClientProxy(ResourceWithMultipleRelated.class)
        .getItems().stream()
        .map(ResourceWithRequiredState::getProperties)
        .findFirst().orElseThrow(() -> new RuntimeException());

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void list_linked_resource_should_be_empty_if_no_links_are_present() throws Exception {

    // create a link with a different relation then defined in the interface
    entryPoint.createLinked(ALTERNATE).setText("item text");

    List<ResourceWithRequiredState> rxLinkedResources = createClientProxy(ResourceWithMultipleRelated.class)
        .getItems();

    assertThat(rxLinkedResources).isEmpty();
  }

  @Test
  public void list_linked_resource_should_emitted_multiple_items() throws Exception {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> entryPoint.createLinked(ITEM).setNumber(i));

    List<TestResourceState> linkedStates = createClientProxy(ResourceWithMultipleRelated.class)
        .getItems().stream()
        .map(ResourceWithRequiredState::getProperties)
        .collect(Collectors.toList());

    assertThat(linkedStates).hasSize(10);
    for (int i = 0; i < numItems; i++) {
      assertThat(linkedStates.get(i).number).isEqualTo(i);
    }
  }

  @Test
  public void list_embedded_resource_should_emitted_single_item() throws Exception {

    entryPoint.createEmbedded(ITEM).setText("item text");

    TestResourceState embeddedState = createClientProxy(ResourceWithMultipleRelated.class)
        .getItems().stream()
        .map(ResourceWithRequiredState::getProperties)
        .findFirst().orElseThrow(() -> new RuntimeException());

    assertThat(embeddedState).isNotNull();
    assertThat(embeddedState.text).isEqualTo("item text");
  }

  @Test
  public void list_embedded_resource_should_be_empty_if_no_resources_are_present() throws Exception {

    // create an embeded resource with a different relation then defined in the interface
    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    List<ResourceWithRequiredState> rxEmbeddedResources = createClientProxy(ResourceWithMultipleRelated.class)
        .getItems();

    assertThat(rxEmbeddedResources).isEmpty();
  }

  @Test
  public void list_embedded_resource_should_emitted_multiple_items() throws Exception {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> entryPoint.createEmbedded(ITEM).setNumber(i));

    List<TestResourceState> embeddedStates = createClientProxy(ResourceWithMultipleRelated.class)
        .getItems().stream()
        .map(ResourceWithRequiredState::getProperties)
        .collect(Collectors.toList());

    assertThat(embeddedStates).hasSize(10);
    for (int i = 0; i < numItems; i++) {
      assertThat(embeddedStates.get(i).number).isEqualTo(i);
    }
  }

  @Test
  public void duplicates_should_be_filtered_if_they_are_linked_and_embedded() throws Exception {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> {
      TestResource item = entryPoint.createLinked(ITEM).setNumber(i);
      entryPoint.asHalResource().addEmbedded(ITEM, item.asHalResource());
    });

    List<TestResourceState> embeddedStates = createClientProxy(ResourceWithMultipleRelated.class)
        .getItems().stream()
        .map(ResourceWithRequiredState::getProperties)
        .collect(Collectors.toList());

    assertThat(embeddedStates).hasSize(10);
    for (int i = 0; i < numItems; i++) {
      assertThat(embeddedStates.get(i).number).isEqualTo(i);
    }
  }

}
