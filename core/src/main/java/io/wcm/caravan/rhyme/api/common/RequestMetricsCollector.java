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
package io.wcm.caravan.rhyme.api.common;

import java.time.Duration;
import java.util.function.Supplier;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalApiClientBuilder;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.server.HalResponseRendererBuilder;
import io.wcm.caravan.rhyme.api.server.RhymeMetadataConfiguration;
import io.wcm.caravan.rhyme.impl.metadata.FullMetadataGenerator;
import io.wcm.caravan.rhyme.impl.metadata.MaxAgeOnlyCollector;

/**
 * Keeps track of all upstream resource that have been fetched while handling the current request, and collects
 * additional data for performance analyze and caching. The collected information will be included in the
 * responses created by {@link Rhyme#renderResponse(LinkableResource)}.
 * <p>
 * You shouldn't have to interact with this interface directly except for advanced integration or testing scenarios.
 * Usually an instance of this interface is automatically created for each {@link Rhyme} instance, and can choose
 * whether you want to include the embedded metadata in the response via
 * {@link RhymeBuilder#withMetadataConfiguration(RhymeMetadataConfiguration)}.
 * </p>
 * <p>
 * If you are not working with {@link RhymeBuilder}, but are integrating {@link HalApiClient} and
 * {@link AsyncHalResponseRenderer} separately, then you must make sure that you create just one instance
 * of {@link RequestMetricsCollector} for each incoming request, and pass it over to both
 * {@link HalApiClientBuilder#withMetrics(RequestMetricsCollector)}
 * and {@link HalResponseRendererBuilder#withMetrics(RequestMetricsCollector)}.
 * </p>
 * @see RhymeMetadataConfiguration
 */
@ProviderType
public interface RequestMetricsCollector {

  /**
   * The name of the query parameter that toggles whether the "rhyme:metadata" resource is embedded when rendering
   * resources. This applies only if you are using one of the framework integrations (e.g. Spring, AEM) in default
   * configuration, where this logic is implemented with a framework specific {@link RhymeMetadataConfiguration}
   */
  public static final String EMBED_RHYME_METADATA = "embedRhymeMetadata";

  /**
   * Calculates the "max-age" Cache-Control header value to be used when rendering the response.
   * @return the minimum "max-age" value of all upstream requests and the limit set via
   *         {@link #setResponseMaxAge(Duration)} (or null if none of this ever haooened)
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
   * @deprecated use {@link #startStopwatch(Class, Supplier)} instead
   */

  @Deprecated
  void onMethodInvocationFinished(Class category, String methodDescription, long invocationDurationMicros);

  /**
   * Start measuring the execution time of a specific (possibly repeated) task within your own code. To finish the
   * measurement, you have to call {@link RequestMetricsStopwatch#close()} when the task has been completed,
   * or use a try-with-resources statement around the code section to be measured (since it's an {@link AutoCloseable}
   * type).
   * The result of your measurements will appear within the embedded "rhyme:metadata" resource in the rendered response.
   * @param measuringClass only used to group the results in the "rhyme:metadata" resource into separate sections for
   *          each class
   * @param taskDescription provides a human readable description of the task (and context) that was executed.
   *          Measurements with the exact same task descriptions will be grouped, and the execution count and overall
   *          sum of execution times will be calculated
   * @return a {@link RequestMetricsStopwatch} that you need to close to finish the measurement
   */
  RequestMetricsStopwatch startStopwatch(Class measuringClass, Supplier<String> taskDescription);

  /**
   * Create a new instance to collect the full data on upstream requests and measure performance for the current
   * incoming request. When this instance is used with {@link AsyncHalResponseRenderer}, a metadata resource with
   * insight on performance and upstream requests will be automatically embedded in the response. If you don't need or
   * want this, then use {@link RequestMetricsCollector#createEssentialCollector()} instead
   * @return a new full-featured instance of {@link RequestMetricsCollector}
   * @see HalApiClientBuilder#withMetrics(RequestMetricsCollector)
   * @see HalResponseRendererBuilder#withMetrics(RequestMetricsCollector)
   */
  static RequestMetricsCollector create() {
    return new FullMetadataGenerator();
  }

  /**
   * Create a minimal implementation that only captures the max-age value of upstream responses, but does
   * not perform any other measurements and will not allow to render any embedded metadata into the response
   * @return a new instance of {@link RequestMetricsCollector} that implements only the essential functionality required
   *         for calculation of the max-age header to work
   */
  static RequestMetricsCollector createEssentialCollector() {
    return new MaxAgeOnlyCollector();
  }
}
