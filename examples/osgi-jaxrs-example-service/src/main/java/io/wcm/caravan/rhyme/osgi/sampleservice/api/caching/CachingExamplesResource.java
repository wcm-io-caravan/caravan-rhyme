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
import io.wcm.caravan.rhyme.api.annotations.TemplateVariables;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionParameters;

/**
 * Examples for resources that need local in-memory caching
 * within the Rhyme framework's internals to avoid multiple identical requests to upstream server
 */
@HalApiInterface
public interface CachingExamplesResource {

  /**
   * Loads a collection of linked or embedded item resources from localhost, and divides it into
   * two embedded collections. This is used to test and verify that (in the server-side implementation
   * of this resource), every required upstream resource is only fetched once, even if you call the same methods on the
   * dynamic client proxies multiple times.
   * @param parameters to be used to the fetch the collection
   * @return a {@link Single} that emits the linked {@link EvenOddItemsResource}
   */
  @Related("examples:evenAndOdd")
  Single<EvenOddItemsResource> getEvenAndOddItems(@TemplateVariables CollectionParameters parameters);
}
