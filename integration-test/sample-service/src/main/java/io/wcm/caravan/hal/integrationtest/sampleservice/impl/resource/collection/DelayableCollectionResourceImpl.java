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
public class DelayableCollectionResourceImpl implements ItemCollectionResource, LinkableResource {

  @Context
  private ExampleServiceRequestContext context;

  @BeanParam
  private CollectionParameters params;

  public DelayableCollectionResourceImpl() {
    // the parameterless constructor required for JAX-RS to instantiate this resource
  }

  DelayableCollectionResourceImpl(ExampleServiceRequestContext context, CollectionParameters parameters) {

    // initialise only the variables that would otherwise be injected by Jax-RS
    this.context = context;
    this.params = parameters;

    // then call the common init method for further initialisation
    this.init();
  }

  @PostConstruct
  void init() {
    // called by Jax-RS after the field injection has happened (or by the parameterized constructor)s
  }

  @Override
  public Single<ItemCollectionResource> getAlternate(Boolean shouldEmbedItems) {

    CollectionParameters alternateParams = new CollectionParameters();
    alternateParams.numItems = params.numItems;
    alternateParams.embedItems = shouldEmbedItems;
    alternateParams.delayMs = params.delayMs;

    return Single.just(new DelayableCollectionResourceImpl(context, alternateParams));
  }

  @Override
  public Observable<ItemResource> getItems() {

    return Observable.range(0, params.numItems)
        .map(index -> new DelayableItemResourceImpl(context, index, params.delayMs).setEmbedded(params.embedItems));
  }

  @Override
  public Single<ExamplesEntryPointResource> getEntryPoint() {

    return Single.just(new ExamplesEntryPointResourceImpl(context));
  }

  @Override
  public Link createLink() {

    String title;
    if (params == null) {
      title = "A link template that allows to specify the number of items in the collection, and whether you want the items to be embedded";
    }
    else if (params.embedItems == null) {
      title = "Choose whether the items should be embedded in the collection (or only linked)";
    }
    else {
      title = "A collection of " + params.numItems + " " + (params.embedItems ? "embedded" : "linked") + " item resources";
      if (params.delayMs > 0) {
        title += " where each item is generated with a simulated delay of " + params.delayMs + "ms";
      }
    }

    return context.buildLinkTo(this)
        .setTitle(title);
  }

  @GET
  public void get(@Suspended AsyncResponse response) {
    context.respondWith(this, response);
  }


}
