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
package io.wcm.caravan.rhyme.api.server;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.impl.renderer.VndErrorResponseRendererImpl;


/**
 * Creates an error response according to the "application/vnd.error+json" media type for any
 * exception thrown and caught during request processing. It will use an {@link ExceptionStatusAndLoggingStrategy}
 * to determine the appropriate status code for the given exception.
 * <p>
 * This renderer is used internally to implement {@link Rhyme#renderResponse(LinkableResource)} and
 * {@link Rhyme#renderVndErrorResponse(Throwable)} but may also be used directly in advanced testing or integration
 * scenarios.
 * </p>
 * @see ExceptionStatusAndLoggingStrategy
 */
@ProviderType
public interface VndErrorResponseRenderer {

  /** the ContentType header used for error responses */
  String CONTENT_TYPE = "application/vnd.error+json";

  /**
   * @param requestUri the URI of the incoming request
   * @param resourceImpl the server-side resource implementation that was being rendered when the error occurred
   * @param error the error that was thrown
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
