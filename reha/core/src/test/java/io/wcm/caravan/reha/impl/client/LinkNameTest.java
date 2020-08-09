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

import static io.wcm.caravan.reha.api.relations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.LinkName;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.annotations.ResourceLink;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.reha.impl.client.ClientTestSupport.ResourceTreeClientTestSupport;
import io.wcm.caravan.reha.impl.client.ResourceStateTest.ResourceWithSingleState;
import io.wcm.caravan.reha.testing.resources.TestResource;
import io.wcm.caravan.reha.testing.resources.TestResourceState;

public class LinkNameTest {

  private final ResourceTreeClientTestSupport client = ClientTestSupport.withResourceTree();
  private final TestResource entryPoint = client.getEntryPoint();

  @HalApiInterface
  interface LinkedResource {

    @ResourceState
    Single<TestResourceState> getState();

    @ResourceLink
    Link createLink();
  }

  @HalApiInterface
  interface ResourceWithNamedLinked {

    @RelatedResource(relation = ITEM)
    Maybe<LinkedResource> getLinkedByName(@LinkName String linkName);
  }

  @Test
  public void should_find_existing_named_link() {

    Observable.range(0, 10).forEach(i -> {
      String linkName = Integer.toString(i);
      entryPoint.createLinked(ITEM, linkName).setNumber(i);
    });

    String linkNameToFind = "5";

    TestResourceState state = client.createProxy(ResourceWithNamedLinked.class)
        .getLinkedByName(linkNameToFind)
        .flatMapSingle(LinkedResource::getState)
        .blockingGet();

    assertThat(state.number).isEqualTo(5);
  }

  @Test
  public void should_return_empty_maybe_for_missing_named_link() {

    Observable.range(0, 10).forEach(i -> {
      String linkName = Integer.toString(i);
      entryPoint.createLinked(ITEM, linkName).setNumber(i);
    });

    String linkNameToFind = "missing";

    Maybe<LinkedResource> maybeLinked = client.createProxy(ResourceWithNamedLinked.class)
        .getLinkedByName(linkNameToFind);

    assertThat(maybeLinked.isEmpty().blockingGet()).isEqualTo(true);
  }

  @Test
  public void should_find_existing_embedded_if_named_link_with_same_href_is_also_present_in_the_resource() {

    Observable.range(0, 10).forEach(i -> {
      String linkName = Integer.toString(i);
      TestResource itemResource = entryPoint.createLinked(ITEM, linkName).setNumber(i);
      entryPoint.asHalResource().addEmbedded(ITEM, itemResource.asHalResource());
    });

    String linkNameToFind = "5";

    TestResourceState state = client.createProxy(ResourceWithNamedLinked.class)
        .getLinkedByName(linkNameToFind)
        .flatMapSingle(LinkedResource::getState)
        .blockingGet();

    assertThat(state.number).isEqualTo(5);
  }

  @Test
  public void should_find_existing_named_linked_even_if_unnamed_embedded_items_are_present() {

    Observable.range(0, 10).forEach(i -> {
      String linkName = Integer.toString(i);
      entryPoint.createLinked(ITEM, linkName).setNumber(i);
      entryPoint.asHalResource().addEmbedded(ITEM, new HalResource());
    });

    String linkNameToFind = "5";

    TestResourceState state = client.createProxy(ResourceWithNamedLinked.class)
        .getLinkedByName(linkNameToFind)
        .flatMapSingle(LinkedResource::getState)
        .blockingGet();

    assertThat(state.number).isEqualTo(5);
  }


  @Test
  public void should_fail_if_null_is_given_as_link_name() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithNamedLinked.class).getLinkedByName(null));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("You must provide a non-null value");
  }


  @HalApiInterface
  interface ResourceWithMultipleAnnotations {

    @RelatedResource(relation = ITEM)
    Single<ResourceWithSingleState> getItem(@LinkName String parameter, @LinkName String other);
  }

  @Test
  public void should_throw_developer_exception_if_multiple_link_name_parameters_are_present() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithMultipleAnnotations.class).getItem("foo", "bar"));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("More than one parameter").hasMessageEndingWith("is annotated with @LinkName");
  }
}
