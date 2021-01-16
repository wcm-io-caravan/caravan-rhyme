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
package io.wcm.caravan.rhyme.impl.client;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.rhyme.impl.client.ClientTestSupport.ENTRY_POINT_URI;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceLink;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.MockClientTestSupport;

public class LazyLoadingTest {

  private static final String RESOURCE_URI = "/linked";

  private final MockClientTestSupport client = ClientTestSupport.withMocking();

  @HalApiInterface
  interface ResourceWithObjectNode {

    @ResourceState
    Single<ObjectNode> getProperties();
  }

  @Test
  public void lazy_loading_should_not_be_triggered_by_calling_resource_state_proxy_method() throws Exception {

    SingleSubject<HalResource> mockJsonResponse = client.mockHalResponseWithSubject(ENTRY_POINT_URI);

    // calling getProperties on the proxy should create the Single
    Single<ObjectNode> rxState = client.createProxy(ResourceWithObjectNode.class).getProperties();
    assertThat(rxState).isNotNull();

    // but no subscriber should have been added yet to to the subject providing the JSON
    assertThat(mockJsonResponse.hasObservers()).isFalse();
  }

  @Test
  public void lazy_loading_should_be_triggered_by_subscription_to_resource_state_single() throws Exception {

    SingleSubject<HalResource> mockJsonResponse = client.mockHalResponseWithSubject(ENTRY_POINT_URI);

    Single<ObjectNode> rxState = client.createProxy(ResourceWithObjectNode.class).getProperties();

    // verify that any subscription on the returned single will result in a subsription to the mock response
    TestObserver<ObjectNode> stateObserver = TestObserver.create();
    rxState.subscribe(stateObserver);
    assertThat(mockJsonResponse.hasObservers()).isTrue();

    // and emitting a json node will lead to completion of the single
    mockJsonResponse.onSuccess(new HalResource());
    stateObserver.assertComplete();
  }

  @HalApiInterface
  interface ResourceWithLink {

    @Related(ITEM)
    Single<ResourceWithObjectNode> getLinked();
  }

  @Test
  public void lazy_loading_should_not_be_triggered_by_calling_related_resource_proxy_method() throws Exception {

    SingleSubject<HalResource> mockEntryPoint = client.mockHalResponseWithSubject(ENTRY_POINT_URI);
    SingleSubject<HalResource> mockLinkedResource = client.mockHalResponseWithSubject(RESOURCE_URI);

    // calling getLinked and getProperties on the proxies should create the Singles
    Single<ResourceWithObjectNode> rxResource = client.createProxy(ResourceWithLink.class).getLinked();
    assertThat(rxResource).isNotNull();
    Single<ObjectNode> rxState = rxResource.flatMap(ResourceWithObjectNode::getProperties);
    assertThat(rxState).isNotNull();

    // but no subscriber should have been added yet to to the subjects providing the JSON
    assertThat(mockEntryPoint.hasObservers()).isFalse();
    assertThat(mockLinkedResource.hasObservers()).isFalse();
  }

  @Test
  public void lazy_loading_should_be_triggered_by_subscription_to_linked_resource_state_single() throws Exception {

    SingleSubject<HalResource> mockEntryPoint = client.mockHalResponseWithSubject(ENTRY_POINT_URI);
    SingleSubject<HalResource> mockLinkedResource = client.mockHalResponseWithSubject(RESOURCE_URI);

    Single<ResourceWithObjectNode> rxLinkedResource = client.createProxy(ResourceWithLink.class).getLinked();
    Single<ObjectNode> rxState = rxLinkedResource.flatMap(ResourceWithObjectNode::getProperties);

    // verify that a subscription on the returned single will first create a subscription to the entry point only
    TestObserver<ObjectNode> stateObserver = TestObserver.create();
    rxState.subscribe(stateObserver);
    assertThat(mockEntryPoint.hasObservers()).isTrue();
    assertThat(mockLinkedResource.hasObservers()).isFalse();

    // then when the entry point is emitted, another subscription to the linked resource should happen
    HalResource entryPoint = new HalResource(ENTRY_POINT_URI).addLinks(ITEM, new Link(RESOURCE_URI));
    mockEntryPoint.onSuccess(entryPoint);
    assertThat(mockLinkedResource.hasObservers()).isTrue();

    // and then when the linked resource is emitted, the state observer will be notified
    mockLinkedResource.onSuccess(new HalResource());
    stateObserver.assertComplete();
  }


  @HalApiInterface
  interface LinkableResource {

    @ResourceLink
    Link createLink();
  }

  @Test
  public void lazy_loading_should_not_be_triggered_by_calling_resource_link_proxy_method_on_entry_point() throws Exception {

    SingleSubject<HalResource> mockJsonResponse = client.mockHalResponseWithSubject(ENTRY_POINT_URI);

    // calling createLink on the proxy should return the link
    Link link = client.createProxy(LinkableResource.class).createLink();
    assertThat(link.getHref()).isEqualTo(ENTRY_POINT_URI);

    // but no subscriber should have been added yet to to the subject providing the JSON
    assertThat(mockJsonResponse.hasObservers()).isFalse();
  }

  @HalApiInterface
  interface LinkingEntryPoint {

    @Related(ITEM)
    Single<LinkableResource> getLinked();
  }

  @Test
  public void only_entrypoint_should_be_lazily_loaded_when_calling_resource_link_proxy_method() throws Exception {

    SingleSubject<HalResource> mockEntryPoint = client.mockHalResponseWithSubject(ENTRY_POINT_URI);
    SingleSubject<HalResource> mockLinkedResource = client.mockHalResponseWithSubject(RESOURCE_URI);

    // calling createLink on the linked resource proxy should return the single
    Single<Link> link = client.createProxy(LinkingEntryPoint.class).getLinked()
        .map(LinkableResource::createLink);

    // but no subscriber should have been added yet to to the subjects providing the JSON
    assertThat(mockEntryPoint.hasObservers()).isFalse();
    assertThat(mockLinkedResource.hasObservers()).isFalse();

    // verify that subscribing to the link single will only add an observer to the entry point
    TestObserver<Link> observer = TestObserver.create();
    link.subscribe(observer);
    assertThat(mockEntryPoint.hasObservers()).isTrue();
    assertThat(mockLinkedResource.hasObservers()).isFalse();

    // even after emitting the entry point, the mocked resource shouldn't be fetched
    HalResource entryPoint = new HalResource(ENTRY_POINT_URI).addLinks(ITEM, new Link(RESOURCE_URI));
    mockEntryPoint.onSuccess(entryPoint);
    assertThat(mockLinkedResource.hasObservers()).isFalse();

    // but the subscription to the resource link should be completed
    observer.assertComplete();
  }
}
