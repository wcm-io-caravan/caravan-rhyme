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

import io.reactivex.Observable;
import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * An example for a class that uses constructor injection of the context and parameters.
 * The advantage is that the JaxRsLinkBuilder can see which field belongs to which param by reflection
 * the disadvantage is that you need multiple constructors, a common init method and cannot use final fields
 */
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
    // the parameterless constructor required for JAX-RS to instantiate this resource
  }

  ItemCollectionResourceImpl(ExampleServiceRequestContext context, Integer numItems, Boolean embedItems) {

    // initialise only the variables that would otherwise be injected by Jax-RS
    this.context = context;
    this.numItems = numItems;
    this.embedItems = embedItems;

    // then call the common init method for further initialisation
    this.init();
  }

  @PostConstruct
  void init() {
    // called by Jax-RS after the field injection has happened (or by the parameterized constructor)s
  }

  @Override
  public Observable<ItemResource> getItems() {

    return Observable.range(0, numItems)
        .map(index -> new ItemResourceImpl(context, index).setEmbedded(embedItems));
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

    return context.buildLinkTo(this)
        .setTitle(title);
  }

  @GET
  public void get(@Suspended AsyncResponse response) {
    context.respondWith(this, response);
  }

}
