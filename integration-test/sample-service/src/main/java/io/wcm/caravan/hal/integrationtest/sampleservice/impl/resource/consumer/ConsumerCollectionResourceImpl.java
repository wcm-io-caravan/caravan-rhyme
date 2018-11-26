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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.consumer;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemState;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.hal.microservices.api.server.EmbeddableResource;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@Path("/consumer/items")
public class ConsumerCollectionResourceImpl implements ItemCollectionResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final Integer numItems;
  private final Integer delayMs;
  private final Boolean embedItems;

  ConsumerCollectionResourceImpl(@Context ExampleServiceRequestContext context,
      @QueryParam("numItems") @DefaultValue(value = "0") Integer numItems,
      @QueryParam("embedItems") @DefaultValue(value = "false") Boolean embedItems,
      @QueryParam("delayMs") @DefaultValue(value = "0") Integer delayMs) {

    this.context = context;
    this.numItems = numItems;
    this.delayMs = delayMs;
    this.embedItems = embedItems;
  }

  @Override
  public Observable<ItemResource> getItems() {

    return context.getUpstreamEntryPoint()
        .getCollectionExamples()
        .flatMap(res -> res.getCollection(numItems, embedItems, delayMs))
        .flatMapObservable(ItemCollectionResource::getItems)
        // .flatMapSingle(ItemResource::getProperties) would be more straight forward here
        // however, that will *not* fetch the ItemResources in parallel, hence the trick to convert
        // to observable so that we can use .concatMapEager
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
    if (numItems == null && embedItems == null) {
      title = "A link template that allows to specify the number of items in the collection, and whether you want the items to be embedded";
    }
    else {
      title = "A collection of " + numItems + " " + (embedItems ? "embedded" : "linked") + " item resources";
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
