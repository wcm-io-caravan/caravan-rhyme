/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.reha.api.common;

import java.time.Duration;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.impl.metadata.ResponseMetadataGenerator;

/**
 * Keeps track of all upstream resource that have been fetched while handling the current-request, and collects
 * additional data for performance analyze and caching.
 */
@ProviderType
public interface RequestMetricsCollector {

  /**
   * Calculates the "max-age" Cache-Control header value to be used when rendering the response.
   * @return the minimum "max-age" value of all upstream requests and the limit set via
   *         {@link #setResponseMaxAge(Duration)}
   */
  Integer getResponseMaxAge();

  /**
   * Set the upper limit for the max-age header of the response to the current incoming request.
   * @param duration the maximum cache duration
   */
  void setResponseMaxAge(Duration duration);

  /**
   * Create a resource with performance analysis data that can be embedded in the response
   * @param resourceImpl the main resource being rendered in the current request
   * @return a {@link HalResource} with detailed performance information for the current request
   */
  HalResource createMetadataResource(LinkableResource resourceImpl);

  /**
   * Internal method called by the framework whenever an upstream resource has been retrieved
   * @param resourceUri the URI of the resource that has been retrieved
   * @param resourceTitle the title of the resource that has been retrieved
   * @param maxAgeSeconds the value of the "max-age" header (or null if not available)
   * @param responseTimeMicros the time in microseconds between initiating the request and finishing parsing the
   *          response
   */
  void onResponseRetrieved(String resourceUri, String resourceTitle, Integer maxAgeSeconds, long responseTimeMicros);

  /**
   * Internal method called by the framework to measure execution times of specific request processing stages
   * @param category a class used to group measurements
   * @param methodDescription describes what task was executed
   * @param invocationDurationMicros the time in microseconds used to execute that task
   */
  void onMethodInvocationFinished(Class category, String methodDescription, long invocationDurationMicros);

  /**
   * Create a new instance to collect performance data for the current incoming request
   * @return a new instance of {@link RequestMetricsCollector}
   */
  static RequestMetricsCollector create() {
    return new ResponseMetadataGenerator();
  }
}
