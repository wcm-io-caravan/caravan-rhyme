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

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceRepresentation;
import io.wcm.caravan.rhyme.caravan.api.CaravanRhyme;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.caching.CachingExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorExamplesResource;

/**
 * The HAL API entry point for the OSGi/JAX-RS example service. This service
 * is used for integration tests for the asynchronous request/response
 * handling using <a href="https://github.com/ReactiveX/RxJava">RxJava 3</a>.
 */
@HalApiInterface
public interface ExamplesEntryPointResource {

  /**
   * Example links to show and test how the Rhyme framework is handling collections of
   * linked or embedded resources, and how multiple resources are
   * requested in parallel and asynchronously (without blocking the main thread).
   * @return a {@link Single} that emits the linked {@link CollectionExamplesResource}
   */
  @Related("examples:collections")
  Single<CollectionExamplesResource> getCollectionExamples();

  /**
   * Examples for resources that need local in-memory caching
   * within the Rhyme framework's internals to avoid multiple identical requests to upstream server
   * @return a {@link Single} that emits the linked {@link CachingExamplesResource}
   */
  @Related("examples:caching")
  Single<CachingExamplesResource> getCachingExamples();

  /**
   * Examples for error handling that show how exceptions are rendered as
   * <a href="https://github.com/blongden/vnd.error">vnd.error</a> resources, and how
   * detailed error information from an upstream response is retained over service boundaries.
   * @return a {@link Single} that emits the linked {@link ErrorExamplesResource}
   */
  @Related("examples:errors")
  Single<ErrorExamplesResource> getErrorExamples();

  /**
   * Only if the entry point was loaded with a URL fingerprinting query parameter, this link will point to a version
   * of the entry point that doesn't use such a parameter.
   * @return a {@link Maybe} that emits a link to the {@link ExamplesEntryPointResource} if the current version
   *         of this resource was loaded with a fingerprinted URL
   */
  @Related("latest-version")
  Maybe<ExamplesEntryPointResource> getLatestVersion();

  /**
   * Only if the entry point was loaded <b>without</b> a URL fingerprinting query parameter, this link will point to a
   * version of the entry point that does use such a parameter.
   * @return a {@link Maybe} that emits a fingerprinted link to the {@link ExamplesEntryPointResource} if the current
   *         version of this resource was not loaded with a fingerprinted URL
   */
  @Related("bookmark")
  Maybe<ExamplesEntryPointResource> getPermalink();

  /**
   * Allows clients using {@link Rhyme#getRemoteResource(String, Class)} or
   * {@link CaravanRhyme#getRemoteResource(String, String, Class)} to process this resource
   * using the generic {@link HalResource} class
   * @return the {@link HalResource} as it was fetched and parsed
   */
  @ResourceRepresentation
  HalResource asHalResource();
}
