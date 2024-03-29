/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
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
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemState;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceRequestContext;

public class ClientCollectionResourceImpl implements ItemCollectionResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final CollectionParametersBean params;

  public ClientCollectionResourceImpl(ExampleServiceRequestContext context, CollectionParametersBean parameters) {
    this.context = context;
    this.params = parameters;
  }

  @Override
  public Maybe<String> getTitle() {

    return Maybe.empty();
  }

  @Override
  public Maybe<ItemCollectionResource> getAlternate() {

    CollectionParametersBean newParams = params.withEmbedItems(!params.getEmbedItems());

    return Maybe.just(new ClientCollectionResourceImpl(context, newParams));
  }

  @Override
  public Observable<ItemResource> getItems() {

    return context.getUpstreamEntryPoint()
        .getCollectionExamples()
        .flatMap(res -> res.getDelayedCollection(params))
        .flatMapObservable(ItemCollectionResource::getItems)
        .map(EmbeddedItemResourceImpl::new);
  }

  @Override
  public Link createLink() {

    String title;
    if (params == null) {
      title = "Load a collection through the HalApiClient, and simulate a delay for each item on the server-side";
    }
    else {
      title = "A collection of " + params.getNumItems() + " " + " item resources that were fetched from "
          + (params.getEmbedItems() ? "embedded" : "linked") + " resources";

      if (params.getDelayMs() > 0) {
        title += " with a server-side delay of " + params.getDelayMs() + "ms";
      }
    }

    return context.buildLinkTo((resource, uriInfo, response) -> resource.getClientCollection(uriInfo, response, params))
        .setTitle(title);
  }

  public static class EmbeddedItemResourceImpl implements ItemResource, EmbeddableResource {

    private final ItemResource resource;

    public EmbeddedItemResourceImpl(ItemResource resource) {
      this.resource = resource;
    }

    @Override
    public Single<ItemState> getProperties() {

      return resource.getProperties()
          .map(ClientItemResourceImpl::cloneStateWithCurrentThread);
    }


    @Override
    public boolean isEmbedded() {
      return true;
    }
  }

}
