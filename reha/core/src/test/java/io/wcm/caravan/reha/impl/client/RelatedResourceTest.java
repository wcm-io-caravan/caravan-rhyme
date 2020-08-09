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
package io.wcm.caravan.reha.impl.client;

import static io.wcm.caravan.reha.api.relations.StandardRelations.ALTERNATE;
import static io.wcm.caravan.reha.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.reha.api.relations.StandardRelations.SECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.reha.impl.client.ClientTestSupport.ResourceTreeClientTestSupport;
import io.wcm.caravan.reha.impl.client.ResourceStateTest.ResourceWithSingleState;
import io.wcm.caravan.reha.testing.TestState;
import io.wcm.caravan.reha.testing.resources.TestResource;
import io.wcm.caravan.reha.testing.resources.TestResourceState;


public class RelatedResourceTest {

  private final ResourceTreeClientTestSupport client = ClientTestSupport.withResourceTree();
  private final TestResource entryPoint = client.getEntryPoint();

  @HalApiInterface
  interface ResourceWithSingleRelated {

    @RelatedResource(relation = ITEM)
    Single<ResourceWithSingleState> getItem();
  }

  @Test
  public void single_linked_resource_should_be_emitted() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    TestResourceState linkedState = client.createProxy(ResourceWithSingleRelated.class)
        .getItem()
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void single_linked_resource_should_fail_if_link_is_not_present() throws Exception {

    entryPoint.createLinked(ALTERNATE).setText("item text");

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithSingleRelated.class).getItem().blockingGet());

    assertThat(ex).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  public void single_embedded_resource_should_be_emitted() throws Exception {

    entryPoint.createEmbedded(ITEM).setText("item text");

    TestResourceState linkedState = client.createProxy(ResourceWithSingleRelated.class)
        .getItem()
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void single_embedded_resource_should_fail_if_resource_is_not_present() throws Exception {

    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithSingleRelated.class).getItem().blockingGet());

    assertThat(ex).isInstanceOf(NoSuchElementException.class);
  }


  @HalApiInterface
  interface ResourceWithOptionalRelated {

    @RelatedResource(relation = ITEM)
    Maybe<ResourceWithSingleState> getOptionalItem();
  }

  @Test
  public void maybe_linked_resource_should_be_emitted() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    TestResourceState linkedState = client.createProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem()
        .flatMapSingle(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void maybe_linked_resource_should_be_empty_if_link_is_not_present() throws Exception {

    // create a link with a different relation then defined in the interface
    entryPoint.createLinked(ALTERNATE).setText("item text");

    Maybe<ResourceWithSingleState> maybeLinked = client.createProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem();

    assertThat(maybeLinked.isEmpty().blockingGet()).isTrue();
  }

  @Test
  public void maybe_embedded_resource_should_be_emitted() throws Exception {

    entryPoint.createEmbedded(ITEM).setText("item text");

    TestResourceState linkedState = client.createProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem()
        .flatMapSingle(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void maybe_embedded_resource_should_be_empty_if_resource_is_not_present() throws Exception {

    // create a link with a different relation then defined in the interface
    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    Maybe<ResourceWithSingleState> maybeEmbedded = client.createProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem();

    assertThat(maybeEmbedded.isEmpty().blockingGet()).isTrue();
  }


  @HalApiInterface
  interface ResourceWithMultipleRelated {

    @RelatedResource(relation = ITEM)
    Observable<ResourceWithSingleState> getItems();
  }

  @Test
  public void observable_linked_resource_should_emitted_single_item() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    TestResourceState linkedState = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .firstOrError()
        .blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  @Test
  public void observable_linked_resource_should_be_empty_if_no_links_are_present() throws Exception {

    // create a link with a different relation then defined in the interface
    entryPoint.createLinked(ALTERNATE).setText("item text");

    Observable<ResourceWithSingleState> rxLinkedResources = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems();

    assertThat(rxLinkedResources.isEmpty().blockingGet()).isTrue();
  }

  @Test
  public void observable_linked_resource_should_emitted_multiple_items() throws Exception {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> entryPoint.createLinked(ITEM).setNumber(i));

    List<TestResourceState> linkedStates = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .toList()
        .blockingGet();

    assertThat(linkedStates).hasSize(10);
    for (int i = 0; i < numItems; i++) {
      assertThat(linkedStates.get(i).number).isEqualTo(i);
    }
  }

  @Test
  public void observable_embedded_resource_should_emitted_single_item() throws Exception {

    entryPoint.createEmbedded(ITEM).setText("item text");

    TestResourceState embeddedState = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .firstOrError()
        .blockingGet();

    assertThat(embeddedState).isNotNull();
    assertThat(embeddedState.text).isEqualTo("item text");
  }

  @Test
  public void observable_embedded_resource_should_be_empty_if_no_resources_are_present() throws Exception {

    // create an embeded resource with a different relation then defined in the interface
    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    Observable<ResourceWithSingleState> rxEmbeddedResources = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems();

    assertThat(rxEmbeddedResources.isEmpty().blockingGet()).isTrue();
  }

  @Test
  public void observable_embedded_resource_should_emitted_multiple_items() throws Exception {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> entryPoint.createEmbedded(ITEM).setNumber(i));

    List<TestResourceState> embeddedStates = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .toList()
        .blockingGet();

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

    List<TestResourceState> embeddedStates = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .toList()
        .blockingGet();

    assertThat(embeddedStates).hasSize(10);
    for (int i = 0; i < numItems; i++) {
      assertThat(embeddedStates.get(i).number).isEqualTo(i);
    }
  }


  @HalApiInterface
  interface ResourceWithPublisherRelated {

    @RelatedResource(relation = ITEM)
    Publisher<ResourceWithSingleState> getItems();
  }

  @Test
  public void related_resource_method_can_return_publisher() throws Exception {

    entryPoint.createLinked(ITEM).setText("item text");

    Observable<ResourceWithSingleState> items = Observable.fromPublisher(client.createProxy(ResourceWithPublisherRelated.class).getItems());
    TestResourceState linkedState = items
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .firstOrError()
        .blockingGet();

    assertThat(linkedState).isNotNull();
    assertThat(linkedState.text).isEqualTo("item text");
  }

  interface ResourceWithoutAnnotation {

    @ResourceState
    Single<TestResourceState> getProperties();
  }

  @HalApiInterface
  interface ResourceWithIllegalAnnotations {

    Single<ResourceWithSingleState> noAnnotation();

    @RelatedResource(relation = ITEM)
    Single<ResourceWithoutAnnotation> getInvalidLinked();

    @RelatedResource(relation = SECTION)
    Single<TestState> notAnInterface();
  }

  @Test
  public void should_throw_developer_exception_if_annotation_is_missing_on_proxy_method() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithIllegalAnnotations.class).noAnnotation());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class).hasMessageContaining("is not annotated with one of the supported HAL API annotations");
  }

  @Test
  public void should_throw_developer_exception_if_annotation_is_missing_on_related_resource_type() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithIllegalAnnotations.class).getInvalidLinked().blockingGet());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageContaining("has an invalid emission type").hasMessageEndingWith("which does not have a @HalApiInterface annotation.");
  }

  @Test
  public void should_throw_developer_exception_if_return_type_does_not_emit_an_interface() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithIllegalAnnotations.class).notAnInterface().blockingGet());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageContaining("has an invalid emission type").hasMessageEndingWith("which does not have a @HalApiInterface annotation.");
  }
}
