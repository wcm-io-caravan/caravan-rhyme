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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.CollectionParameters;

public class CollectionParametersImpl implements CollectionParameters {

  @QueryParam("numItems")
  private Integer numItems;

  @QueryParam("embedItems")
  @DefaultValue(value = "false")
  private Boolean embedItems;

  @QueryParam("delayMs")
  @DefaultValue(value = "0")
  private Integer delayMs;

  @Override
  public Integer getNumItems() {
    return this.numItems;
  }

  @Override
  public Boolean getEmbedItems() {
    return this.embedItems;
  }

  @Override
  public Integer getDelayMs() {
    return this.delayMs;
  }

  static CollectionParametersImpl clone(CollectionParameters other) {

    if (other == null) {
      return null;
    }

    CollectionParametersImpl cloned = new CollectionParametersImpl();

    cloned.numItems = other.getNumItems();
    cloned.embedItems = other.getEmbedItems();
    cloned.delayMs = other.getDelayMs();

    return cloned;
  }

  public CollectionParametersImpl withNumItems(Integer value) {
    CollectionParametersImpl cloned = clone(this);
    cloned.numItems = value;
    return cloned;
  }

  public CollectionParametersImpl withEmbedItems(Boolean value) {
    CollectionParametersImpl cloned = clone(this);
    cloned.embedItems = value;
    return cloned;
  }

  public CollectionParametersImpl withDelayMs(Integer value) {
    CollectionParametersImpl cloned = clone(this);
    cloned.delayMs = value;
    return cloned;
  }
}
