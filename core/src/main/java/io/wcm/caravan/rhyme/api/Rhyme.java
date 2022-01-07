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
package io.wcm.caravan.rhyme.api;

import java.time.Duration;
import java.util.function.Supplier;

import org.osgi.annotation.versioning.ProviderType;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

/**
 * A facade interface that simplifies all interaction with the core framework to handle an incoming request for a
 * HAL+JSON resource. You should create a {@link Rhyme} instance immediately after
 * accepting an incoming request (using a {@link RhymeBuilder}) and then use this single instance to fetch all upstream
 * resource and render the response.
 * <p>
 * If you are only using the Rhyme framework as a HAL client library (but not to render HAL resources) then you can use
 * {@link HalApiClient} directly.
 * </p>
 * @see RhymeBuilder
 */
@ProviderType
public interface Rhyme {

  /**
   * Create a dynamic client proxy to load and navigate through HAL+JSON resources from an upstream service.
   * Any interaction with the proxies will be recorded and used to generate embedded metadata about the upstream
   * requests when {@link #renderResponse(LinkableResource)} is called later.
   * @param <T> an interface annotated with {@link HalApiInterface}
   * @param uri the URI of the entry point, in any format that the {@link HalResourceLoader} being used can understand
   * @param halApiInterface an interface annotated with {@link HalApiInterface}
   * @return a dynamic proxy instance of the provided interface that you can use to navigate through the
   *         resources of the service
   * @see HalApiClient
   */
  <T> T getRemoteResource(String uri, Class<T> halApiInterface);

  /**
   * Limit the maximum time for which the response should be cached by clients and downstream services. Note that
   * calling this method only sets the upper limit: if other upstream resource fetched during the current request
   * indicate a lower max-age value in their header, that lower value will be used instead.
   * @param duration the max cache time
   */
  void setResponseMaxAge(Duration duration);

  /**
   * Asynchronously render the given resource as a {@link HalResponse} instance. If rendering is successful, that
   * instance will have a 200 status code and a HAL+JSON media type. If any errors were thrown and handled, a vnd.error
   * response will be rendered instead (using the status code obtained from the
   * {@link ExceptionStatusAndLoggingStrategy})
   * @param resourceImpl a server-side implementation of an interface annotated with {@link HalApiInterface}
   * @return a {@link Single} that will emit a {@link HalResponse} instance with status code, content type
   *         and body already set
   */
  Single<HalResponse> renderResponse(LinkableResource resourceImpl);

  /**
   * Render a vnd.error response for an error that happened before the {@link LinkableResource} instance was created
   * (using the status code obtained from the {@link ExceptionStatusAndLoggingStrategy}).
   * @param error an exception that was caught during initialisation
   * @return a {@link HalResponse} instance with status code, content type and body already set
   */
  HalResponse renderVndErrorResponse(Throwable error);

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

}
