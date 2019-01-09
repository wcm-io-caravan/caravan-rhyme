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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.caching;

import static io.wcm.caravan.hal.microservices.util.RxJavaTransformers.filterWith;

import java.util.function.Function;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.caching.EvenOddItemsResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.TitledState;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.ClientCollectionResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.CollectionParametersImpl;
import io.wcm.caravan.hal.microservices.api.server.EmbeddableResource;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@Path("/caching/evenAndOdd")
public class EvenAndOddItemsResourceImpl implements EvenOddItemsResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final CollectionParametersImpl params;

  public EvenAndOddItemsResourceImpl(@Context ExampleServiceRequestContext context,
      @BeanParam CollectionParametersImpl parameters) {

    this.context = context;
    this.params = parameters;
  }


  @Override
  public Single<ItemCollectionResource> getEvenItems() {

    Observable<ItemResource> rxEvenItems = fetchAndFilter(this::isEvenItem);

    return Single.just(new EmbeddedCollectionResourceImpl(rxEvenItems));
  }

  private Single<Boolean> isEvenItem(ItemResource resource) {
    return resource.getProperties().map(state -> state.index % 2 == 0);
  }

  @Override
  public Single<ItemCollectionResource> getOddItems() {

    Observable<ItemResource> rxOddItems = fetchAndFilter(this::isOddItem);

    return Single.just(new EmbeddedCollectionResourceImpl(rxOddItems));
  }

  private Single<Boolean> isOddItem(ItemResource resource) {
    return resource.getProperties().map(state -> state.index % 2 == 1);
  }

  private Observable<ItemResource> fetchAllItems() {

    return context.getUpstreamEntryPoint()
        .flatMap(ExamplesEntryPointResource::getCollectionExamples)
        .flatMap(r -> r.getCollection(params))
        .flatMapObservable(ItemCollectionResource::getItems);
  }

  private Observable<ItemResource> fetchAndFilter(Function<ItemResource, Single<Boolean>> filterFunc) {

    return fetchAllItems()
        .compose(filterWith(filterFunc));
  }

  @Override
  public Single<ExamplesEntryPointResource> getEntryPoint() {

    return Single.just(new ExamplesEntryPointResourceImpl(context));
  }

  @Override
  public Link createLink() {

    String title = "An example that loads a collection from upstream service, and separates it into even and odd items";

    return context.buildLinkTo(this).setTitle(title);
  }

  @GET
  public void get(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {
    context.respondWith(this, uriInfo, response);
  }

  static class EmbeddedCollectionResourceImpl implements ItemCollectionResource, EmbeddableResource {

    private final Observable<ItemResource> items;

    EmbeddedCollectionResourceImpl(Observable<ItemResource> items) {
      this.items = items;
    }

    @Override
    public Observable<ItemResource> getItems() {
      return items.map(ClientCollectionResourceImpl.EmbeddedItemResourceImpl::new);
    }

    @Override
    public Maybe<TitledState> getState() {

      return items.count()
          .map(numItems -> new TitledState().withTitle("A collection of " + numItems + " items"))
          .toMaybe();
    }

    @Override
    public Maybe<ItemCollectionResource> getAlternate(Boolean embedItems) {
      return Maybe.empty();
    }

    @Override
    public boolean isEmbedded() {
      return true;
    }

  }

}
