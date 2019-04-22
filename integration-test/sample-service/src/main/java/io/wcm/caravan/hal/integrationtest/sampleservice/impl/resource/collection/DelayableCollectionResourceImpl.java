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

import static io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl.SERVICE_PATH;

import javax.annotation.PostConstruct;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.TitledState;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * An example for a class that uses constructor injection of the context and parameters.
 * The advantage is that the JaxRsLinkBuilder can see which field belongs to which param by reflection
 * the disadvantage is that you need multiple constructors, a common init method and cannot use final fields
 */
@Component(service = DelayableCollectionResourceImpl.class, scope = ServiceScope.PROTOTYPE)
@JaxrsResource
@Path(SERVICE_PATH + "/collections/items")
public class DelayableCollectionResourceImpl implements ItemCollectionResource, LinkableResource {

  @Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
  private ExampleServiceRequestContext context;

  @BeanParam
  private CollectionParametersImpl params;

  public DelayableCollectionResourceImpl() {
    // the parameterless constructor required for JAX-RS to instantiate this resource
  }

  DelayableCollectionResourceImpl(ExampleServiceRequestContext context, CollectionParametersImpl parameters) {

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
  public Maybe<ItemCollectionResource> getAlternate(Boolean shouldEmbedItems) {
    return Maybe.just(new DelayableCollectionResourceImpl(context, params.withEmbedItems(shouldEmbedItems)));
  }

  @Override
  public Observable<ItemResource> getItems() {

    return Observable.range(0, params.getNumItems())
        .map(index -> new DelayableItemResourceImpl(context, index, params.getDelayMs()).setEmbedded(params.getEmbedItems()));
  }

  @Override
  public Maybe<TitledState> getState() {
    return Maybe.empty();
  }

  @Override
  public Link createLink() {

    String title;
    if (params == null) {
      title = "A link template that allows to specify the number of items in the collection, and whether you want the items to be embedded";
    }
    else if (params.getEmbedItems() == null) {
      title = "Choose whether the items should be embedded in the collection (or only linked)";
    }
    else {
      title = "A collection of " + params.getNumItems() + " " + (params.getEmbedItems() ? "embedded" : "linked") + " item resources";
      if (params.getDelayMs() > 0) {
        title += " where each item is generated with a simulated delay of " + params.getDelayMs() + "ms";
      }
    }

    return context.buildLinkTo(this)
        .setTitle(title);
  }

  @GET
  public void get(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {
    context.respondWith(uriInfo, this, response);
  }


}
