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
package io.wcm.caravan.reha.api.server;

import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.common.HalResponse;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.impl.renderer.VndErrorResponseRendererImpl;


/**
 * Creates an error response according to the "application/vnd.error+json" media type for any
 * exception thrown when rendering resources with {@link AsyncHalResourceRenderer#renderResource(LinkableResource)}
 */
public interface VndErrorResponseRenderer {

  /** the ContentType header used for error responses */
  String CONTENT_TYPE = "application/vnd.error+json";

  /**
   * @param requestUri the URI of the incoming request
   * @param resourceImpl a server-side implementation instance of an interface annotated with {@link HalApiInterface}
   * @param error the exception that was emitted by {@link AsyncHalResourceRenderer#renderResource(LinkableResource)}
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance and caching information for
   *          the current incoming request
   * @return a HalResponse with status code and a HAL body with detailed error information
   */
  HalResponse renderError(String requestUri, LinkableResource resourceImpl, Throwable error, RequestMetricsCollector metrics);

  /**
   * @param exceptionStrategy allows to control the status code and logging of exceptions
   * @return a new instance of {@link VndErrorResponseRenderer}
   */
  static VndErrorResponseRenderer create(ExceptionStatusAndLoggingStrategy exceptionStrategy) {

    return new VndErrorResponseRendererImpl(exceptionStrategy);
  }
}
