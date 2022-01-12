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

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariables;

/**
 * Example links to show and test how the Rhyme framework is handling collections of
 * linked or embedded resources, and how multiple resources are
 * requested in parallel and asynchronously (without blocking the main thread).
 * <p>
 * The resources with the "delayed:" relations will simply generate some simple test
 * resources on the server side, but allow you to control the minimum response time
 * with a query parameter.
 * </p>
 * <p>
 * The resources with the "proxy:" relations will be <b>consuming</b> those generated
 * test resources: in the background there will be HTTP requests to the API
 * on localhost using Rhyme's dynamic client proxies. To check what
 * exactly is happening under the hood, look into the embedded "rhyme:metadata" resources.
 * </p>
 */
@HalApiInterface
public interface CollectionExamplesResource {

  /**
   * Generate a collection of simple linked or embedded test resources
   * with a configurable delay to simulate a slow response time. Note that
   * the delay will only be applied to the linked item resource, not to
   * the collection itself (unless you chose to embed the items in the collection).
   * @param options defined by {@link CollectionParameters}
   * @return a {@link Single} that emits the linked {@link ItemCollectionResource}
   */
  @Related("delayed:collection")
  Single<ItemCollectionResource> getDelayedCollection(
      @TemplateVariables CollectionParameters options);

  /**
   * Generate a simple test resource with a configurable
   * delay to simulate a slow response time.
   * @param index a numeric id for the item
   * @param delayMs the minimum response time of the resource
   * @return a {@link Single} that emits the linked {@link ItemResource}
   */
  @Related("delayed:item")
  Single<ItemResource> getDelayedItem(
      @TemplateVariable("index") Integer index,
      @TemplateVariable("delayMs") Integer delayMs);

  /**
   * Fetch an collection resource generated via the <b>delayed:collection</b>
   * link through the Rhyme framework's HTTP client proxies.
   * Specifying a large number of items with delayed responses allows you to verify
   * that the upstream resources are indeed fetched in parallel. Note that the items
   * will always be embedded in the <b>output</b> of this <b>proxied</b> resource, but you can control
   * whether embedded resources are used in the original <b>upstream</b> response (and can compare
   * how this affects performance).
   * @param options defined by {@link CollectionParameters}
   * @return a {@link Single} that emits the linked {@link ItemCollectionResource}
   */
  @Related("proxy:collection")
  Single<ItemCollectionResource> getCollectionThroughClient(
      @TemplateVariables CollectionParameters options);

  /**
   * Fetch an item resource generated via the <b>delayed:item</b>
   * link through the Rhyme framework's HTTP client proxies. You can verify that client-side
   * caching works as expected by reloading the resource with the same parameters.
   * Since the upstream resource will be taken from cache immediately, the longer
   * response time simulated by the upstream resource does not apply.
   * @param index a numeric id for the item
   * @param delayMs the minimum response time of the <b>upstream</b> resource
   * @return a {@link Single} that emits the linked {@link ItemResource}
   */
  @Related("proxy:item")
  Single<ItemResource> getItemThroughClient(
      @TemplateVariable("index") Integer index,
      @TemplateVariable("delayMs") Integer delayMs);
}
