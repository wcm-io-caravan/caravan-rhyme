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
package io.wcm.caravan.rhyme.osgi.sampleservice.api;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.caching.CachingExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorExamplesResource;

@HalApiInterface
public interface ExamplesEntryPointResource {

  @ResourceState
  Maybe<ObjectNode> getState();

  @Related("examples:collections")
  Single<CollectionExamplesResource> getCollectionExamples();

  @Related("examples:caching")
  Single<CachingExamplesResource> getCachingExamples();

  @Related("examples:errors")
  Single<ErrorExamplesResource> getErrorExamples();
}
