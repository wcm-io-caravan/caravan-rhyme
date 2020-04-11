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
public class DelayableCollectionResourceImpl implements ItemCollectionResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final CollectionParametersImpl params;

  public DelayableCollectionResourceImpl(ExampleServiceRequestContext context, CollectionParametersImpl parameters) {
    this.context = context;
    this.params = parameters;
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

    return context.buildLinkTo((resource, uriInfo, response) -> resource.getDelayableCollection(uriInfo, response, params))
        .setTitle(title);
  }
}
