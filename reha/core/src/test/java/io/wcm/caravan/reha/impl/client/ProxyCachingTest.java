/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
import static io.wcm.caravan.reha.impl.client.ClientTestSupport.ENTRY_POINT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.annotations.ResourceRepresentation;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.client.HalApiClient;
import io.wcm.caravan.reha.api.common.HalResponse;
import io.wcm.caravan.reha.api.relations.StandardRelations;
import io.wcm.caravan.reha.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.reha.impl.client.ClientTestSupport.MockClientTestSupport.SubscriberCounter;
import io.wcm.caravan.reha.testing.LinkableTestResource;
import io.wcm.caravan.reha.testing.TestState;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
public class ProxyCachingTest {

  private static final String ITEM_1_URL = "/item/1";
  private static final String ITEM_2_URL = "/item/2";
  private static final String ALT_1_URL = "/alt/1";

  private final MockClientTestSupport client = ClientTestSupport.withMocking();
  private final HalResource entryPointHal = new HalResource(ENTRY_POINT_URI);

  SubscriberCounter entryPointCounter;
  SubscriberCounter item1Counter;
  SubscriberCounter item2Counter;

  @BeforeEach
  void setUp() {
    entryPointCounter = client.mockHalResponse(ENTRY_POINT_URI, entryPointHal);

    item1Counter = addLinkAndMockResource(ITEM_1_URL);
    item2Counter = addLinkAndMockResource(ITEM_2_URL);
  }

  private SubscriberCounter addLinkAndMockResource(String itemUrl) {
    TestState state = new TestState(itemUrl);
    HalResource linkedItem = new HalResource(state, itemUrl);
    entryPointHal.addLinks(ITEM, new Link(itemUrl));
    return client.mockHalResponse(itemUrl, linkedItem);
  }

  @HalApiInterface
  interface EntryPoint {

    @ResourceState
    Maybe<TestState> getState();

    @RelatedResource(relation = ITEM)
    Observable<LinkableTestResource> getLinked();

    @RelatedResource(relation = ALTERNATE)
    Observable<LinkableTestResource> getAlternate();

    @ResourceRepresentation
    Single<HalResource> asHalResource();
  }

  @Test
  public void multiple_calls_to_entrypoint_should_return_the_same_proxy_instance_for_the_same_interface() {

    HalApiClient halApiClient = client.getHalApiClient();

    EntryPoint proxy1 = halApiClient.getEntryPoint(ENTRY_POINT_URI, EntryPoint.class);
    EntryPoint proxy2 = halApiClient.getEntryPoint(ENTRY_POINT_URI, EntryPoint.class);

    assertThat(proxy1).isSameAs(proxy2);

    verifyZeroInteractions(client.getMockJsonLoader());
  }

  @HalApiInterface
  interface AltEntryPointInterface {

    @ResourceState
    Maybe<TestState> getState();
  }

  @Test
  public void multiple_calls_to_entrypoint_should_not_return_the_same_proxy_instance_for_a_different_interface() {

    HalApiClient halApiClient = client.getHalApiClient();

    EntryPoint proxy1 = halApiClient.getEntryPoint(ENTRY_POINT_URI, EntryPoint.class);
    AltEntryPointInterface proxy2 = halApiClient.getEntryPoint(ENTRY_POINT_URI, AltEntryPointInterface.class);

    assertThat(proxy1).isNotSameAs(proxy2);

    verifyZeroInteractions(client.getMockJsonLoader());
  }

  @Test
  public void multiple_calls_to_state_method_should_return_the_same_observable() {

    EntryPoint entryPoint = client.createProxy(EntryPoint.class);

    Maybe<TestState> state1 = entryPoint.getState();
    Maybe<TestState> state2 = entryPoint.getState();

    assertThat(state1).isSameAs(state2);

    verifyZeroInteractions(client.getMockJsonLoader());
  }

  @Test
  public void multiple_calls_to_related_resource_method_should_return_the_same_observable() {

    EntryPoint entryPoint = client.createProxy(EntryPoint.class);

    Observable<LinkableTestResource> linked1 = entryPoint.getLinked();
    Observable<LinkableTestResource> linked2 = entryPoint.getLinked();

    assertThat(linked1).isSameAs(linked2);

    verifyZeroInteractions(client.getMockJsonLoader());
  }

  @Test
  public void subscriber_counter_should_count_subscriptions_correctly() {
    Single<HalResponse> item1 = item1Counter.getCountingSingle();

    item1.blockingGet();
    item1.blockingGet();

    assertThat(item1Counter.getCount()).isEqualTo(2);
  }

  @Test
  public void observables_from_multiple_calls_emit_the_same_caching_instances() {

    EntryPoint entryPoint = client.createProxy(EntryPoint.class);

    Observable<LinkableTestResource> linked1 = entryPoint.getLinked();
    Observable<LinkableTestResource> linked2 = entryPoint.getLinked();

    List<TestState> list1 = linked1.concatMapMaybe(LinkableTestResource::getState).toList().blockingGet();
    List<TestState> list2 = linked2.concatMapMaybe(LinkableTestResource::getState).toList().blockingGet();

    assertThat(list1).containsExactlyElementsOf(list2);

    // verify that json for each item was only loaded once
    verify(client.getMockJsonLoader()).loadJsonResource(ENTRY_POINT_URI);
    verify(client.getMockJsonLoader()).loadJsonResource(ITEM_1_URL);
    verify(client.getMockJsonLoader()).loadJsonResource(ITEM_2_URL);
    verifyNoMoreInteractions(client.getMockJsonLoader());

    assertThat(entryPointCounter.getCount()).isEqualTo(1);
    assertThat(item1Counter.getCount()).isEqualTo(1);
    assertThat(item2Counter.getCount()).isEqualTo(1);
  }

  @Test
  public void following_different_links_from_entry_point_will_load_entry_point_only_once() {

    entryPointHal.addLinks(StandardRelations.ALTERNATE, new Link(ALT_1_URL));

    EntryPoint entryPoint = client.createProxy(EntryPoint.class);

    LinkableTestResource linked = entryPoint.getLinked().blockingFirst();
    LinkableTestResource alternate = entryPoint.getAlternate().blockingFirst();

    assertThat(linked).isNotSameAs(alternate);

    // verify that entrypoint was only loaded once
    verify(client.getMockJsonLoader()).loadJsonResource(ENTRY_POINT_URI);
    assertThat(entryPointCounter.getCount()).isEqualTo(1);
  }

  @Test
  public void following_different_links_to_same_resource_will_load_that_resource_only_once() {

    entryPointHal.addLinks(StandardRelations.ALTERNATE, new Link(ITEM_1_URL));

    EntryPoint entryPoint = client.createProxy(EntryPoint.class);

    TestState item1 = entryPoint.getLinked().concatMapMaybe(LinkableTestResource::getState).blockingFirst();
    TestState alt1 = entryPoint.getAlternate().concatMapMaybe(LinkableTestResource::getState).blockingFirst();

    // since the both resourcse have exactly the same link and interface, the same proxy instance will be returned
    assertThat(item1).isSameAs(alt1);

    assertThat(entryPointCounter.getCount()).isEqualTo(1);
    assertThat(item1Counter.getCount()).isEqualTo(1);
  }

  @Test
  public void following_differently_named_links_will_create_different_proxies_but_not_load_resources_twice() {

    entryPointHal.addLinks(StandardRelations.ALTERNATE, new Link(ITEM_1_URL).setName("foo"));

    EntryPoint entryPoint = client.createProxy(EntryPoint.class);

    TestState item1 = entryPoint.getLinked().concatMapMaybe(LinkableTestResource::getState).blockingFirst();
    TestState alt1 = entryPoint.getAlternate().concatMapMaybe(LinkableTestResource::getState).blockingFirst();

    // since the second link has a different name, a dfferent proxy instance will be returned
    assertThat(item1).isNotSameAs(alt1);

    // but JSON resources are still only loaded once
    assertThat(entryPointCounter.getCount()).isEqualTo(1);
    assertThat(item1Counter.getCount()).isEqualTo(1);
  }

  @Test
  public void caching_should_work_if_two_subscriptions_haben_before_emission() {

    entryPointHal.addLinks(StandardRelations.ALTERNATE, new Link(ALT_1_URL));

    SingleSubject<HalResource> subject = client.mockHalResponseWithSubject(ALT_1_URL);

    EntryPoint entryPoint = client.createProxy(EntryPoint.class);

    Observable<TestState> alt1 = entryPoint.getAlternate().concatMapMaybe(LinkableTestResource::getState);
    Observable<TestState> alt2 = entryPoint.getAlternate().concatMapMaybe(LinkableTestResource::getState);


    assertThat(subject.hasObservers()).isFalse();

    TestObserver<TestState> subscriber1 = TestObserver.create();
    alt1.subscribe(subscriber1);
    TestObserver<TestState> subscriber2 = TestObserver.create();
    alt2.subscribe(subscriber2);

    assertThat(subject.hasObservers()).isTrue();
  }
}
