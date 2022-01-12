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
package io.wcm.caravan.rhyme.osgi.sampleservice.api.caching;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemCollectionResource;

/**
 * A resource that fetches a collection of item resources from localhost,
 * and divides it into two collections.
 */
@HalApiInterface
public interface EvenOddItemsResource {

  /**
   * a collection that contains all the item resources with an even index property
   * @return a {@link Single} that emits the embedded {@link ItemCollectionResource}
   */
  @Related("example:evenItems")
  Single<ItemCollectionResource> getEvenItems();

  /**
   * a collection that contains all the item resources with an odd index property
   * @return a {@link Single} that emits the embedded {@link ItemCollectionResource}
   */
  @Related("example:oddItems")
  Single<ItemCollectionResource> getOddItems();
}
