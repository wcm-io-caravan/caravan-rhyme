/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.caravan.reha.api;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.common.HalResponse;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.reha.api.spi.JsonResourceLoader;

/**
 * A facade interface that simplifies all interaction with the core framework to handle an incoming request for a
 * HAL+JSON resource. You should create a {@link Reha} instance immediately after
 * accepting an incoming request (using a {@link RehaBuilder}) and then use this single instance to fetch all upstream
 * resource and render the response.
 */
public interface Reha {

  /**
   * Create a dynamic client proxy to load HAL+JSON resources from an upstream service.
   * @param <T> an interface annotated with {@link HalApiInterface}
   * @param uri the URI of the entry point, in any format that the {@link JsonResourceLoader} being used can understand
   * @param halApiInterface an interface annotated with {@link HalApiInterface}
   * @return a dynamic proxy instance of the provided {@link HalApiInterface} that you can use to navigate through the
   *         resources of the service
   */
  <T> T getUpstreamEntryPoint(String uri, Class<T> halApiInterface);

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
   * @return a {@link CompletionStage} that will provide a {@link HalResponse} instance with status code, content type
   *         and body already set
   */
  CompletionStage<HalResponse> renderResponseAsync(LinkableResource resourceImpl);

  /**
   * Synchronously render the given resource as a {@link HalResponse} instance. If rendering is successful, that
   * instance will have a 200 status code and a HAL+JSON media type. If any errors were thrown and handled, a vnd.error
   * response will be rendered instead (using the status code obtained from the
   * {@link ExceptionStatusAndLoggingStrategy})
   * @param resourceImpl a server-side implementation of an interface annotated with {@link HalApiInterface}
   * @return a {@link HalResponse} instance with status code, content type and body already set
   */
  HalResponse renderResponse(LinkableResource resourceImpl);

  /**
   * Render a vnd.error response for an error that happened before the {@link LinkableResource} instance was created
   * (using the status code obtained from the {@link ExceptionStatusAndLoggingStrategy}).
   * @param error an exception that was caught during initialisation
   * @return a {@link HalResponse} instance with status code, content type and body already set
   */
  HalResponse renderVndErrorResponse(Throwable error);
}
