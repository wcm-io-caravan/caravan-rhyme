/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;
import io.wcm.caravan.rhyme.impl.RhymeBuilders;

/**
 * A fluent builder for {@link AsyncHalResponseRenderer} instances which is only used in advanced integration
 * scenarios. As a regular user of the framework, simply use {@link RhymeBuilder} instead.
 * @see RhymeBuilder
 * @see Rhyme#renderResponse(LinkableResource)
 */
@ProviderType
public interface HalResponseRendererBuilder {

  /**
   * @return a new builder instance with no customizations applied
   */
  static HalResponseRendererBuilder create() {
    return RhymeBuilders.renderer();
  }

  /**
   * Provide the {@link RequestMetricsCollector} instance that was used to capture and measure
   * all interaction with the {@link HalApiClient} for the current incoming request.
   * @param metricsSharedWithClient the same instance that was used to create the {@link HalApiClient}
   * @return this
   */
  HalResponseRendererBuilder withMetrics(RequestMetricsCollector metricsSharedWithClient);

  /**
   * Enable generation of "curies" links (to HTML documentation generated with the rhyme-docs-maven-plugin)
   * when rendering {@link HalResponse}s with {@link Rhyme#renderResponse(LinkableResource)}.
   * If you call this method, you also have to ensure that you actually serve the generated HTML files using
   * {@link RhymeDocsSupport#loadGeneratedHtml(RhymeDocsSupport, String)}
   * @param rhymeDocsSupport the SPI instance that handles loading of the generated HTML
   * @return this
   */
  HalResponseRendererBuilder withRhymeDocsSupport(RhymeDocsSupport rhymeDocsSupport);

  /**
   * Extend the core framework to support additional return types in your annotated HAL API interfaces.
   * You can call this method multiple times if you want to register more than one extension.
   * @param additionalTypeSupport extension to the default type support
   * @return this
   */
  HalResponseRendererBuilder withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport);

  /**
   * Extend the core framework to support additional annotation types in your annotated HAL API interfaces.
   * You can call this method multiple times if you want to register more than one extension.
   * @param additionalTypeSupport extension to the default type support
   * @return this
   */
  HalResponseRendererBuilder withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport);

  /**
   * Allow the exception handling of the core framework to support additional platform / framework specific exceptions.
   * You can call this method multiple times if you want to register more than one extension.
   * @param customStrategy extension to the default exception handling.
   * @return this
   */
  HalResponseRendererBuilder withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy);

  /**
   * Replace the default Jackson {@link ObjectMapper} used for JSON serialization with a customized instance
   * @param objectMapper a configured {@link ObjectMapper} instance (which can be a single shared instance)
   * @return this
   */
  HalResponseRendererBuilder withObjectMapper(ObjectMapper objectMapper);

  /**
   * @return the new {@link AsyncHalResponseRenderer} instance
   */
  AsyncHalResponseRenderer build();
}
