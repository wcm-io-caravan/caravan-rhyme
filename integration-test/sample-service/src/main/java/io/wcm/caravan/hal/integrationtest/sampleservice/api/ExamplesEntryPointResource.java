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
package io.wcm.caravan.hal.integrationtest.sampleservice.api;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.caching.CachingExamplesResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.CollectionExamplesResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.errors.ErrorExamplesResource;

@HalApiInterface
public interface ExamplesEntryPointResource {

  @RelatedResource(relation = "examples:collections")
  Single<CollectionExamplesResource> getCollectionExamples();

  @RelatedResource(relation = "examples:caching")
  Single<CachingExamplesResource> getCachingExamples();

  @RelatedResource(relation = "examples:errors")
  Single<ErrorExamplesResource> getErrorExamples();
}
