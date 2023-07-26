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

import javax.ws.rs.BeanParam;
import javax.ws.rs.QueryParam;

import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionParameters;

/**
 * An implementation of the {@link CollectionParameters} interface that
 * contains JAX-RS annotations, so it can be used as a {@link BeanParam}
 * argument in a resource method signature
 */
public class CollectionParametersBean implements CollectionParameters {

  @QueryParam("numItems")
  private int numItems;

  @QueryParam("embedItems")
  private boolean embedItems;

  @QueryParam("delayMs")
  private int delayMs;

  @Override
  public int getNumItems() {
    return this.numItems;
  }

  @Override
  public boolean getEmbedItems() {
    return this.embedItems;
  }

  @Override
  public int getDelayMs() {
    return this.delayMs;
  }

  private static CollectionParametersBean clone(CollectionParameters other) {

    CollectionParametersBean cloned = new CollectionParametersBean();

    cloned.numItems = other.getNumItems();
    cloned.embedItems = other.getEmbedItems();
    cloned.delayMs = other.getDelayMs();

    return cloned;
  }

  public CollectionParametersBean withNumItems(int value) {
    CollectionParametersBean cloned = clone(this);
    cloned.numItems = value;
    return cloned;
  }

  public CollectionParametersBean withEmbedItems(boolean value) {
    CollectionParametersBean cloned = clone(this);
    cloned.embedItems = value;
    return cloned;
  }

  public CollectionParametersBean withDelayMs(int value) {
    CollectionParametersBean cloned = clone(this);
    cloned.delayMs = value;
    return cloned;
  }
}
