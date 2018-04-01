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

import javax.annotation.PostConstruct;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import io.wcm.caravan.hal.api.common.LinkableResource;
import io.wcm.caravan.hal.api.server.jaxrs.AsyncHalResponseHandler;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.hal.resource.Link;
import rx.Observable;

@Path("/collections/items")
public class ItemCollectionResourceImpl implements ItemCollectionResource, LinkableResource {

  @Context
  private ExampleServiceRequestContext context;

  @QueryParam("numItems")
  @DefaultValue(value = "0")
  private Integer numItems;

  @QueryParam("embedItems")
  @DefaultValue(value = "false")
  private Boolean embedItems;

  public ItemCollectionResourceImpl() {

  }

  ItemCollectionResourceImpl(ExampleServiceRequestContext context, Integer numItems, Boolean embedItems) {
    this.context = context;
    this.numItems = numItems;
    this.embedItems = embedItems;

    this.init();
  }

  @PostConstruct
  void init() {
    System.out.println("init was called for item collection");
  }

  @Override
  public Observable<ItemResource> getItems() {

    return Observable.range(0, numItems)
        .map(index -> new ItemResourceImpl(context, index).setEmbedded(embedItems));
  }

  @Override
  public Observable<ExamplesEntryPointResource> getEntryPoint() {

    return Observable.just(new ExamplesEntryPointResourceImpl(context));
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

    return context.buildLinkTo(this)
        .setTitle(title);
  }

  @GET
  public void get(@Suspended AsyncResponse response) {
    AsyncHalResponseHandler.respond(this, response);
  }

}
