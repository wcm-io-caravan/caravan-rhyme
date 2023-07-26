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

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRenderer;

/**
 * Asynchronously creates a {@link HalResponse} from a server-side {@link HalApiInterface} implementation instance,
 * using {@link AsyncHalResourceRenderer} to render a {@link HalResource}, and {@link VndErrorResponseRenderer} to
 * handle any errors that might happen during resource rendering.
 * <p>
 * This renderer is used internally to implement {@link Rhyme#renderResponse(LinkableResource)}, but may also be used
 * directly in advanced testing or integration scenarios.
 * </p>
 * @see AsyncHalResourceRenderer
 * @see VndErrorResponseRenderer
 */
@ProviderType
public interface AsyncHalResponseRenderer {

  /**
   * Asynchronously render the given resource as a {@link HalResponse} instance. If rendering is successful, that
   * instance will have a 200 status code and a HAL+JSON media type. If any errors were thrown and handled, a vnd.error
   * response will be rendered instead (using the status code obtained from the
   * {@link ExceptionStatusAndLoggingStrategy})
   * @param requestUri the URI of the incoming request
   * @param resourceImpl a server-side implementation instance of an interface annotated with {@link HalApiInterface}
   * @return a {@link Single} that emits a {@link HalResponse} with initialized properties
   */
  Single<HalResponse> renderResponse(String requestUri, LinkableResource resourceImpl);

  /**
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance and caching information for
   *          the current incoming request
   * @param exceptionStrategy allows controlling the status code and logging of exceptions being thrown during rendering
   * @return a new {@link AsyncHalResponseRenderer} to use for the current incoming request
   * @deprecated Use {@link HalResponseRendererBuilder} instead
   */
  @Deprecated
  static AsyncHalResponseRenderer create(RequestMetricsCollector metrics, ExceptionStatusAndLoggingStrategy exceptionStrategy) {

    return HalResponseRendererBuilder.create()
        .withMetrics(metrics)
        .withExceptionStrategy(exceptionStrategy)
        .build();
  }

  /**
   * Alternative factory method that allows to support different HAL API annotations or method return types
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance and caching information for
   *          the current incoming request
   * @param exceptionStrategy allows controlling the status code and logging of exceptions being thrown during rendering
   * @param annotationSupport an (optional) strategy to identify HAL API interfaces and methods that use different
   *          annotations
   * @param returnTypeSupport an (optional) strategy to support additional return types in your HAL API interface
   *          methods
   * @return a new {@link AsyncHalResponseRenderer} to use for the current incoming request
   * @deprecated Use {@link HalResponseRendererBuilder} instead
   */
  @Deprecated
  static AsyncHalResponseRenderer create(RequestMetricsCollector metrics, ExceptionStatusAndLoggingStrategy exceptionStrategy,
      HalApiAnnotationSupport annotationSupport, HalApiReturnTypeSupport returnTypeSupport) {

    return HalResponseRendererBuilder.create()
        .withMetrics(metrics)
        .withExceptionStrategy(exceptionStrategy)
        .withAnnotationTypeSupport(annotationSupport)
        .withReturnTypeSupport(returnTypeSupport)
        .build();
  }
}
