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
package io.wcm.caravan.rhyme.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;
import io.wcm.caravan.rhyme.impl.client.HalApiClientImpl;
import io.wcm.caravan.rhyme.impl.reflection.CompositeHalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupportAdapter;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRenderer;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererImpl;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResponseRendererImpl;
import io.wcm.caravan.rhyme.impl.renderer.CompositeExceptionStatusAndLoggingStrategy;

class CommonRhymeBuilderImpl<BuilderInterface> {

  private HalResourceLoader resourceLoader;

  private RequestMetricsCollector metrics;

  private final List<ExceptionStatusAndLoggingStrategy> exceptionStrategies = new ArrayList<>();

  private final List<HalApiTypeSupport> typeSupports = new ArrayList<>();

  private RhymeDocsSupport rhymeDocsSupport;

  public BuilderInterface withResourceLoader(HalResourceLoader resourceLoader) {

    this.resourceLoader = resourceLoader;
    return (BuilderInterface)this;
  }

  public BuilderInterface withMetrics(RequestMetricsCollector metricsSharedWithClient) {

    this.metrics = metricsSharedWithClient;
    return (BuilderInterface)this;
  }

  public BuilderInterface withRhymeDocsSupport(RhymeDocsSupport rhymeDocsSupport) {

    this.rhymeDocsSupport = rhymeDocsSupport;
    return (BuilderInterface)this;
  }

  public BuilderInterface withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport) {

    if (additionalTypeSupport != null) {
      typeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    }
    return (BuilderInterface)this;
  }

  public BuilderInterface withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport) {

    if (additionalTypeSupport != null) {
      typeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    }
    return (BuilderInterface)this;
  }

  public BuilderInterface withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy) {

    exceptionStrategies.add(customStrategy);
    return (BuilderInterface)this;
  }

  AsyncHalResponseRenderer buildAsyncRenderer() {

    metrics = ObjectUtils.defaultIfNull(metrics, RequestMetricsCollector.create());

    HalApiTypeSupport typeSupport = getEffectiveTypeSupport();

    AsyncHalResourceRenderer resourceRenderer = new AsyncHalResourceRendererImpl(metrics, typeSupport);

    ExceptionStatusAndLoggingStrategy exceptionStrategy = getEffectiveExceptionStrategy();

    return new AsyncHalResponseRendererImpl(resourceRenderer, metrics, exceptionStrategy, typeSupport, rhymeDocsSupport);
  }

  HalApiClient buildApiClient() {

    if (resourceLoader == null) {
      resourceLoader = HalResourceLoader.withDefaultHttpClient();
    }

    if (metrics == null) {
      metrics = RequestMetricsCollector.create();
    }

    HalApiTypeSupport effectiveTypeSupport = getEffectiveTypeSupport();

    return new HalApiClientImpl(resourceLoader, metrics, effectiveTypeSupport);
  }

  Rhyme buildRhyme(String incomingRequestUri) {

    if (resourceLoader == null) {
      resourceLoader = HalResourceLoader.withDefaultHttpClient();
    }

    HalApiTypeSupport typeSupport = getEffectiveTypeSupport();
    ExceptionStatusAndLoggingStrategy exceptionStrategy = getEffectiveExceptionStrategy();

    return new RhymeImpl(incomingRequestUri, resourceLoader, exceptionStrategy, typeSupport, rhymeDocsSupport);
  }

  private ExceptionStatusAndLoggingStrategy getEffectiveExceptionStrategy() {

    List<ExceptionStatusAndLoggingStrategy> nonNullStrategies = exceptionStrategies.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    if (nonNullStrategies.isEmpty()) {
      return null;
    }
    if (nonNullStrategies.size() == 1) {
      return nonNullStrategies.get(0);
    }

    return new CompositeExceptionStatusAndLoggingStrategy(nonNullStrategies);
  }

  private HalApiTypeSupport getEffectiveTypeSupport() {

    DefaultHalApiTypeSupport defaultSupport = new DefaultHalApiTypeSupport();

    if (typeSupports.isEmpty()) {
      return defaultSupport;
    }

    List<HalApiTypeSupport> customAndDefault = new ArrayList<>(typeSupports);
    customAndDefault.add(defaultSupport);

    return new CompositeHalApiTypeSupport(customAndDefault);
  }

}
