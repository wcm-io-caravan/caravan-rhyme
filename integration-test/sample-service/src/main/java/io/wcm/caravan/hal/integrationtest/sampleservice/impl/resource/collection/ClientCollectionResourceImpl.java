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
import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemState;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.TitledState;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.microservices.api.server.EmbeddableResource;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@Component(service = ClientCollectionResourceImpl.class, scope = ServiceScope.PROTOTYPE)
@JaxrsResource
@Path(SERVICE_PATH + "/collection/client/items")
public class ClientCollectionResourceImpl implements ItemCollectionResource, LinkableResource {

  @Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
  private ExampleServiceRequestContext context;

  @BeanParam
  private CollectionParametersImpl params;

  public ClientCollectionResourceImpl() {

  }

  public ClientCollectionResourceImpl(ExampleServiceRequestContext context, CollectionParametersImpl parameters) {
    this.context = context;
    this.params = parameters;
  }

  @Override
  public Maybe<ItemCollectionResource> getAlternate(Boolean shouldEmbedItems) {

    return Maybe.just(new ClientCollectionResourceImpl(context, params.withEmbedItems(shouldEmbedItems)));
  }

  @Override
  public Observable<ItemResource> getItems() {

    return context.getUpstreamEntryPoint()
        .flatMap(ExamplesEntryPointResource::getCollectionExamples)
        .flatMap(res -> res.getCollection(params))
        .flatMapObservable(ItemCollectionResource::getItems)
        .map(EmbeddedItemResourceImpl::new);
  }

  @Override
  public Maybe<TitledState> getState() {
    return Maybe.empty();
  }

  @Override
  public Link createLink() {

    String title;
    if (params == null) {
      title = "Load a collection through the HalApiClient, and simulate a delay for each item on the server-side";
    }
    else if (params.getEmbedItems() == null) {
      title = "Choose whether the items should already be embedded on the server-side";
    }
    else {
      title = "A collection of " + params.getNumItems() + " " + " item resources that were fetched from "
          + (params.getEmbedItems() ? "embedded" : "linked") + " resources";

      if (params.getDelayMs() > 0) {
        title += " with a server-side delay of " + params.getDelayMs() + "ms";
      }
    }

    return context.buildLinkTo(this).setTitle(title);
  }

  @GET
  public void get(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {
    context.respondWith(uriInfo, this, response);
  }

  public static class EmbeddedItemResourceImpl implements ItemResource, EmbeddableResource {

    private final ItemResource resource;

    public EmbeddedItemResourceImpl(ItemResource resource) {
      this.resource = resource;
    }

    @Override
    public Single<ItemState> getProperties() {

      return resource.getProperties()
          .map(clientState -> {

            ItemState itemState = new ItemState();
            itemState.title = clientState.title;
            itemState.index = clientState.index;
            itemState.thread = Thread.currentThread().getName();

            return itemState;
          });
    }

    @Override
    public boolean isEmbedded() {
      return true;
    }
  }

}
