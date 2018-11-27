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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.CollectionParameters;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemState;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.hal.microservices.api.server.EmbeddableResource;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@Path("/collection/client/items")
public class ClientCollectionResourceImpl implements ItemCollectionResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final CollectionParameters params;

  public ClientCollectionResourceImpl(@Context ExampleServiceRequestContext context,
      @BeanParam CollectionParameters parameters) {

    this.context = context;
    this.params = parameters;
  }

  @Override
  public Single<ItemCollectionResource> getAlternate(Boolean shouldEmbedItems) {

    CollectionParameters alternateParams = new CollectionParameters();
    alternateParams.numItems = params.numItems;
    alternateParams.embedItems = shouldEmbedItems;
    alternateParams.delayMs = params.delayMs;

    return Single.just(new ClientCollectionResourceImpl(context, alternateParams));
  }

  @Override
  public Observable<ItemResource> getItems() {

    return context.getUpstreamEntryPoint()
        .getCollectionExamples()
        .flatMap(res -> res.getCollection(params))
        .flatMapObservable(ItemCollectionResource::getItems)
        // using .concatMapSingle here would be more straight forward, but it would lead to the resources
        // being loaded in parallel (while concatMapEager will subscribe to all of them asap)
        .concatMapEager(item -> item.getProperties().toObservable())
        .map(EmbeddedItemResourceImpl::new);
  }

  @Override
  public Single<ExamplesEntryPointResource> getEntryPoint() {

    return Single.just(new ExamplesEntryPointResourceImpl(context));
  }

  @Override
  public Link createLink() {

    String title;
    if (params == null) {
      title = "Load a collection through the HalApiClient, and simulate a delay for each item on the server-side";
    }
    else if (params.embedItems == null) {
      title = "Choose whether the items should already be embedded on the server-side";
    }
    else {
      title = "A collection of " + params.numItems + " " + " item resources that were fetched from "
          + (params.embedItems ? "embedded" : "linked") + " resources";

      if (params.delayMs > 0) {
        title += " with a server-side delay of " + params.delayMs + "ms";
      }
    }

    return context.buildLinkTo(this).setTitle(title);
  }

  @GET
  public void get(@Suspended AsyncResponse response) {
    context.respondWith(this, response);
  }

  private static class EmbeddedItemResourceImpl implements ItemResource, EmbeddableResource {

    private final ItemState state;

    EmbeddedItemResourceImpl(ItemState state) {
      this.state = state;
    }

    @Override
    public Single<ItemState> getProperties() {
      return Single.just(state);
    }

    @Override
    public boolean isEmbedded() {
      return true;
    }
  }

}
