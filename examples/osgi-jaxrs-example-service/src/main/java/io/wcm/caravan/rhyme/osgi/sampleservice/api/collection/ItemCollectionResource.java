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
package io.wcm.caravan.rhyme.osgi.sampleservice.api.collection;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.relations.StandardRelations;

/**
 * Defines the common structure and available link relations for all collections of test resources in this example
 * service.
 */
@HalApiInterface
public interface ItemCollectionResource {

  /**
   * @return an optional state with just a title (to be displayed in the HAL browser when this resource is embedded)
   */
  @ResourceState
  Maybe<TitledState> getState();

  /**
   * Allows to switch between two versions of this resource: one that is based on embedded resource items, and one that
   * is only using links.
   * @return a {@link Maybe} that emits the same {@link ItemCollectionResource} with different parameters
   */
  @Related(StandardRelations.ALTERNATE)
  Maybe<ItemCollectionResource> getAlternate();

  /**
   * The test resources contained in this collection. Depending on the parameters used to fetch this
   * resource, the item resources may be embedded or only linked.
   * @return an {@link Observable} that emits all linked (or embedded) {@link ItemResource} instances
   */
  @Related("examples:item")
  Observable<ItemResource> getItems();
}
