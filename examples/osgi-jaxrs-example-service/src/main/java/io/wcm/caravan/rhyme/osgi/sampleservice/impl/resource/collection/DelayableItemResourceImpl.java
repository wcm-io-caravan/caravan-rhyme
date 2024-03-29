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

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemState;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceRequestContext;


/**
 * An example for a class that uses constructor injection of the context and parameters.
 * The advantage is that you only have a single constructor and final fields,
 * the disadvantage is that the JaxRsLinkBuilder must assume that the fields have the exact same name as the parameters
 */
public class DelayableItemResourceImpl implements ItemResource, LinkableResource, EmbeddableResource {

  private final ExampleServiceRequestContext context;

  private final Integer index;
  private final Integer delayMs;
  private boolean embedded;

  public DelayableItemResourceImpl(ExampleServiceRequestContext context, Integer index, Integer delayMs) {
    this.context = context;
    this.index = index;
    this.delayMs = delayMs;
  }

  @Override
  public Single<ItemState> getProperties() {

    ItemState item = new ItemState();
    item.title = getTitle();
    item.index = index;
    item.thread = Thread.currentThread().getName();

    Single<ItemState> properties = Single.just(item);
    if (delayMs != null && delayMs > 0) {
      properties = properties.delay(delayMs, TimeUnit.MILLISECONDS)
          .map(state -> {
            state.thread = Thread.currentThread().getName();
            return state;
          });
    }

    return properties;
  }

  @Override
  public Link createLink() {

    return context
        .buildLinkTo((resource, uriInfo, response) -> resource.getDelayableItem(uriInfo, response, index, delayMs))
        .setTitle(getTitle());
  }

  private String getTitle() {
    String title;
    if (index != null) {
      title = "The item with index " + index;
      if (delayMs != null && delayMs.intValue() > 0) {
        title += " delayed by " + delayMs + "ms";
      }
    }
    else {
      title = "Load an item with a specific index, and simulate a delay during resource generation";
    }
    return title;
  }

  @Override
  public boolean isEmbedded() {

    return embedded;
  }

  DelayableItemResourceImpl setEmbedded(boolean value) {
    this.embedded = value;
    return this;
  }
}
