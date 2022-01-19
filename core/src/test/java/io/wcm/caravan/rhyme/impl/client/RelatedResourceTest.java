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
package io.wcm.caravan.rhyme.impl.client;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ALTERNATE;
import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.rhyme.api.relations.StandardRelations.SECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.ResourceTreeClientTestSupport;
import io.wcm.caravan.rhyme.impl.client.ResourceStateTest.ResourceWithSingleState;
import io.wcm.caravan.rhyme.testing.TestState;
import io.wcm.caravan.rhyme.testing.resources.TestResource;
import io.wcm.caravan.rhyme.testing.resources.TestResourceState;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
class RelatedResourceTest {

  private final ResourceTreeClientTestSupport client = ClientTestSupport.withResourceTree();
  private final TestResource entryPoint = client.getEntryPoint();

  @HalApiInterface
  interface ResourceWithSingleRelated {

    @Related(ITEM)
    Single<ResourceWithSingleState> getItem();
  }

  @Test
  void single_linked_resource_should_be_emitted() {

    entryPoint.createLinked(ITEM).setText("item text");

    TestResourceState linkedState = client.createProxy(ResourceWithSingleRelated.class)
        .getItem()
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState)
        .isNotNull();

    assertThat(linkedState.text)
        .isEqualTo("item text");
  }

  @Test
  void single_linked_resource_should_fail_if_link_is_not_present() {

    entryPoint.createLinked(ALTERNATE).setText("item text");

    Single<ResourceWithSingleState> linkedResource = client.createProxy(ResourceWithSingleRelated.class).getItem();

    Throwable ex = assertThrows(NoSuchElementException.class, linkedResource::blockingGet);

    assertThat(ex)
        .hasMessage(null);
  }

  @Test
  void single_embedded_resource_should_be_emitted() {

    entryPoint.createEmbedded(ITEM).setText("item text");

    TestResourceState linkedState = client.createProxy(ResourceWithSingleRelated.class)
        .getItem()
        .flatMap(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState)
        .isNotNull();

    assertThat(linkedState.text)
        .isEqualTo("item text");
  }

  @Test
  void single_embedded_resource_should_fail_if_resource_is_not_present() {

    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    Single<ResourceWithSingleState> embeddedResource = client.createProxy(ResourceWithSingleRelated.class).getItem();

    Throwable ex = assertThrows(NoSuchElementException.class, embeddedResource::blockingGet);

    assertThat(ex)
        .hasMessage(null);
  }


  @HalApiInterface
  interface ResourceWithOptionalRelated {

    @Related(ITEM)
    Maybe<ResourceWithSingleState> getOptionalItem();
  }

  @Test
  void maybe_linked_resource_should_be_emitted() {

    entryPoint.createLinked(ITEM).setText("item text");

    TestResourceState linkedState = client.createProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem()
        .flatMapSingle(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState)
        .isNotNull();

    assertThat(linkedState.text)
        .isEqualTo("item text");
  }

  @Test
  void maybe_linked_resource_should_be_empty_if_link_is_not_present() {

    // create a link with a different relation then defined in the interface
    entryPoint.createLinked(ALTERNATE).setText("item text");

    Maybe<ResourceWithSingleState> maybeLinked = client.createProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem();

    assertThat(maybeLinked.isEmpty().blockingGet())
        .isTrue();
  }

  @Test
  void maybe_embedded_resource_should_be_emitted() {

    entryPoint.createEmbedded(ITEM).setText("item text");

    TestResourceState linkedState = client.createProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem()
        .flatMapSingle(ResourceWithSingleState::getProperties)
        .blockingGet();

    assertThat(linkedState)
        .isNotNull();

    assertThat(linkedState.text)
        .isEqualTo("item text");
  }

  @Test
  void maybe_embedded_resource_should_be_empty_if_resource_is_not_present() {

    // create a link with a different relation then defined in the interface
    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    Maybe<ResourceWithSingleState> maybeEmbedded = client.createProxy(ResourceWithOptionalRelated.class)
        .getOptionalItem();

    assertThat(maybeEmbedded.isEmpty().blockingGet())
        .isTrue();
  }


  @HalApiInterface
  interface ResourceWithMultipleRelated {

    @Related(ITEM)
    Observable<ResourceWithSingleState> getItems();
  }

  @Test
  void observable_linked_resource_should_emitted_single_item() {

    entryPoint.createLinked(ITEM).setText("item text");

    TestResourceState linkedState = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .firstOrError()
        .blockingGet();

    assertThat(linkedState)
        .isNotNull();

    assertThat(linkedState.text)
        .isEqualTo("item text");
  }

  @Test
  void observable_linked_resource_should_be_empty_if_no_links_are_present() {

    // create a link with a different relation then defined in the interface
    entryPoint.createLinked(ALTERNATE).setText("item text");

    Observable<ResourceWithSingleState> rxLinkedResources = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems();

    assertThat(rxLinkedResources.isEmpty().blockingGet())
        .isTrue();
  }

  @Test
  void observable_linked_resource_should_emitted_multiple_items() {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> entryPoint.createLinked(ITEM).setNumber(i));

    List<TestResourceState> linkedStates = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .toList()
        .blockingGet();

    assertThat(linkedStates)
        .hasSize(10);

    for (int i = 0; i < numItems; i++) {
      assertThat(linkedStates.get(i).number)
          .isEqualTo(i);
    }
  }

  @Test
  void observable_embedded_resource_should_emitted_single_item() {

    entryPoint.createEmbedded(ITEM).setText("item text");

    TestResourceState embeddedState = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .firstOrError()
        .blockingGet();

    assertThat(embeddedState)
        .isNotNull();

    assertThat(embeddedState.text)
        .isEqualTo("item text");
  }

  @Test
  void observable_embedded_resource_should_be_empty_if_no_resources_are_present() {

    // create an embeded resource with a different relation then defined in the interface
    entryPoint.createEmbedded(ALTERNATE).setText("item text");

    Observable<ResourceWithSingleState> rxEmbeddedResources = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems();

    assertThat(rxEmbeddedResources.isEmpty().blockingGet())
        .isTrue();
  }

  @Test
  void observable_embedded_resource_should_emitted_multiple_items() {

    int numItems = 10;
    Observable.range(0, numItems).forEach(i -> entryPoint.createEmbedded(ITEM).setNumber(i));

    List<TestResourceState> embeddedStates = client.createProxy(ResourceWithMultipleRelated.class)
        .getItems()
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .toList()
        .blockingGet();

    assertThat(embeddedStates)
        .hasSize(10);

    for (int i = 0; i < numItems; i++) {
      assertThat(embeddedStates.get(i).number)
          .isEqualTo(i);
    }
  }

  @Test
  void duplicates_should_be_filtered_if_they_are_linked_and_embedded() {

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

    assertThat(embeddedStates)
        .hasSize(10);

    for (int i = 0; i < numItems; i++) {
      assertThat(embeddedStates.get(i).number)
          .isEqualTo(i);
    }
  }


  @HalApiInterface
  interface ResourceWithPublisherRelated {

    @Related(ITEM)
    Publisher<ResourceWithSingleState> getItems();
  }

  @Test
  void related_resource_method_can_return_publisher() {

    entryPoint.createLinked(ITEM).setText("item text");

    Observable<ResourceWithSingleState> items = Observable.fromPublisher(client.createProxy(ResourceWithPublisherRelated.class).getItems());
    TestResourceState linkedState = items
        .concatMapSingle(ResourceWithSingleState::getProperties)
        .firstOrError()
        .blockingGet();

    assertThat(linkedState)
        .isNotNull();

    assertThat(linkedState.text)
        .isEqualTo("item text");
  }

  interface ResourceWithoutAnnotation {

    @ResourceState
    Single<TestResourceState> getProperties();
  }

  @HalApiInterface
  interface ResourceWithIllegalAnnotations {

    Single<ResourceWithSingleState> noAnnotation();

    @Related(ITEM)
    Single<ResourceWithoutAnnotation> getInvalidLinked();

    @Related(SECTION)
    Single<TestState> notAnInterface();
  }

  @Test
  void related_resource_method_can_return_links_directly() {

    TestResource linkedItem = entryPoint.createLinked(ITEM);

    Link link = client.createProxy(ResourceWithLinkReturnType.class).getItem().blockingGet();

    assertThat(link.getHref())
        .isEqualTo(linkedItem.getUrl());
  }

  @HalApiInterface
  interface ResourceWithLinkReturnType {

    @Related(ITEM)
    Single<Link> getItem();
  }

  @Test
  void should_throw_developer_exception_if_annotation_is_missing_on_proxy_method() {

    ResourceWithIllegalAnnotations proxy = client.createProxy(ResourceWithIllegalAnnotations.class);

    Throwable ex = assertThrows(HalApiDeveloperException.class, proxy::noAnnotation);

    assertThat(ex)
        .hasMessageContaining("is not annotated with one of the supported HAL API annotations");
  }

  @Test
  void should_throw_developer_exception_if_annotation_is_missing_on_related_resource_type() {

    ResourceWithIllegalAnnotations proxy = client.createProxy(ResourceWithIllegalAnnotations.class);

    Single<ResourceWithoutAnnotation> resource = proxy.getInvalidLinked();

    Throwable ex = assertThrows(HalApiDeveloperException.class, resource::blockingGet);

    assertThat(ex)
        .hasMessageContaining("has an invalid emission type")
        .hasMessageEndingWith("which does not have a @HalApiInterface annotation.");
  }

  @Test
  void should_throw_developer_exception_if_return_type_does_not_emit_an_interface() {

    ResourceWithIllegalAnnotations proxy = client.createProxy(ResourceWithIllegalAnnotations.class);

    Single<TestState> returnValue = proxy.notAnInterface();

    Throwable ex = assertThrows(HalApiDeveloperException.class, returnValue::blockingGet);

    assertThat(ex)
        .hasMessageContaining("has an invalid emission type")
        .hasMessageEndingWith("which does not have a @HalApiInterface annotation.");
  }
}
