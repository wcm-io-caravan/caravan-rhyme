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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;

import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;
import io.wcm.caravan.rhyme.impl.RhymeBuilderUtils;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupportAdapter;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRenderer;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererImpl;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResponseRendererImpl;

public final class HalResponseRendererBuilder {

  private RequestMetricsCollector metrics;

  private final List<ExceptionStatusAndLoggingStrategy> exceptionStrategies = new ArrayList<>();

  private final List<HalApiTypeSupport> typeSupports = new ArrayList<>();

  private RhymeDocsSupport rhymeDocsSupport;

  public HalResponseRendererBuilder withMetrics(RequestMetricsCollector metricsSharedWithClient) {

    this.metrics = metricsSharedWithClient;
    return this;
  }

  public HalResponseRendererBuilder withRhymeDocsSupport(RhymeDocsSupport rhymeDocsSupport) {

    this.rhymeDocsSupport = rhymeDocsSupport;
    return this;
  }

  public HalResponseRendererBuilder withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport) {

    if (additionalTypeSupport != null) {
      typeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    }
    return this;
  }

  public HalResponseRendererBuilder withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport) {

    if (additionalTypeSupport != null) {
      typeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    }
    return this;
  }

  public HalResponseRendererBuilder withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy) {

    exceptionStrategies.add(customStrategy);
    return this;
  }

  public AsyncHalResponseRenderer build() {

    metrics = ObjectUtils.defaultIfNull(metrics, RequestMetricsCollector.create());

    HalApiTypeSupport typeSupport = RhymeBuilderUtils.getEffectiveTypeSupport(typeSupports);

    AsyncHalResourceRenderer resourceRenderer = new AsyncHalResourceRendererImpl(metrics, typeSupport);

    ExceptionStatusAndLoggingStrategy exceptionStrategy = RhymeBuilderUtils.getEffectiveExceptionStrategy(exceptionStrategies);

    return new AsyncHalResponseRendererImpl(resourceRenderer, metrics, exceptionStrategy, typeSupport, rhymeDocsSupport);
  }
}
