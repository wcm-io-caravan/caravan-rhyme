/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
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
package io.wcm.caravan.rhyme.impl.client.blocking;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ALTERNATE;
import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.client.ErrorHandlingTest;
import io.wcm.caravan.rhyme.impl.client.blocking.ResourceStateTest.ResourceWithRequiredState;
import io.wcm.caravan.ryhme.testing.resources.TestResource;
import io.wcm.caravan.ryhme.testing.resources.TestResourceState;
import io.wcm.caravan.ryhme.testing.resources.TestResourceTree;

/**
 * Variation of the tests in {@link io.wcm.caravan.rhyme.impl.client.RelatedResourceTest}
 * for blocking HAL API interfaces (i.e. that are not using reactive return types for their methods)
 */
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
public class RelatedResourceTest {

  private RequestMetricsCollector metrics;
  private HalResourceLoader jsonLoader;
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
    T clientProxy = client.getRemoteResource(entryPoint.getUrl(), halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }


  @HalApiInterface
  interface ResourceWithSingleRelated {

    @Related(ITEM)
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

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class).hasMessageStartingWith("The invocation of ResourceWithSingleRelated#getItem() has failed");

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

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class).hasMessageStartingWith("The invocation of ResourceWithSingleRelated#getItem() has failed");
  }


  @HalApiInterface
  interface ResourceWithOptionalRelated {

    @Related(ITEM)
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

    @Related(ITEM)
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

  @HalApiInterface
  interface ResourceWithStreamRelated {

    @Related(ITEM)
    Stream<ResourceWithRequiredState> getItems();
  }

  @Test
  public void related_method_with_stream_return_type_should_be_supported() throws Exception {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> entryPoint.createEmbedded(ITEM).setNumber(i));

    List<ResourceWithRequiredState> embedded = createClientProxy(ResourceWithStreamRelated.class)
        .getItems()
        .collect(Collectors.toList());

    assertThat(embedded).hasSize(numItems);
  }

  @Test
  public void related_method_with_stream_return_type_can_be_called_multiple_times() throws Exception {

    int numItems = 3;
    Observable.range(0, numItems).forEach(i -> entryPoint.createEmbedded(ITEM).setNumber(i));

    ResourceWithStreamRelated resource = createClientProxy(ResourceWithStreamRelated.class);

    List<ResourceWithRequiredState> embedded1 = resource.getItems()
        .collect(Collectors.toList());

    List<ResourceWithRequiredState> embedded2 = resource.getItems()
        .collect(Collectors.toList());

    assertThat(embedded1).containsExactlyElementsOf(embedded2);
  }

  @Test
  public void calling_hashCode_on_client_proxy_throws_exception() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    ResourceWithRequiredState linkedResource = createClientProxy(ResourceWithSingleRelated.class)
        .getItem();

    Throwable ex = catchThrowable(() -> linkedResource.hashCode());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("You cannot call hashCode() on dynamic client proxies.");
  }
}
