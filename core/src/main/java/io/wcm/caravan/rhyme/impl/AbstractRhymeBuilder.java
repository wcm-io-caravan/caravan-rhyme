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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalApiClientBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.server.HalResponseRendererBuilder;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
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

/**
 * An abstract base class for the common customization and dependency injection required by the {@link RhymeBuilder},
 * {@link HalApiClientBuilder} and {@link HalResponseRendererBuilder} implementations.
 * @param <BuilderInterface> the interface that the subclass is implementing
 */
abstract class AbstractRhymeBuilder<BuilderInterface> {

  private HalResourceLoader resourceLoader;

  private RequestMetricsCollector metrics;

  private final List<ExceptionStatusAndLoggingStrategy> exceptionStrategies = new ArrayList<>();

  private final List<HalApiTypeSupport> typeSupports = new ArrayList<>();

  private RhymeDocsSupport rhymeDocsSupport;

  protected boolean wasUsedToBuild;

  @SuppressWarnings("unchecked")
  public BuilderInterface withResourceLoader(HalResourceLoader resourceLoader) {

    this.resourceLoader = resourceLoader;
    return (BuilderInterface)this;
  }

  @SuppressWarnings("unchecked")
  public BuilderInterface withMetrics(RequestMetricsCollector sharedMetrics) {

    this.metrics = sharedMetrics;
    return (BuilderInterface)this;
  }

  @SuppressWarnings("unchecked")
  public BuilderInterface withRhymeDocsSupport(RhymeDocsSupport rhymeDocsSupport) {

    this.rhymeDocsSupport = rhymeDocsSupport;
    return (BuilderInterface)this;
  }

  @SuppressWarnings("unchecked")
  public BuilderInterface withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport) {

    if (additionalTypeSupport != null) {
      typeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    }
    return (BuilderInterface)this;
  }

  @SuppressWarnings("unchecked")
  public BuilderInterface withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport) {

    if (additionalTypeSupport != null) {
      typeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    }
    return (BuilderInterface)this;
  }

  @SuppressWarnings("unchecked")
  public BuilderInterface withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy) {

    exceptionStrategies.add(customStrategy);
    return (BuilderInterface)this;
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

  private void applyDefaultsBeforeBuilding() {

    if (wasUsedToBuild) {
      throw new HalApiDeveloperException("You shouldn't re-use this builder to create more than once instance");
    }

    if (resourceLoader == null) {
      resourceLoader = HalResourceLoader.create();
    }

    if (metrics == null) {
      metrics = RequestMetricsCollector.create();
    }
  }

  AsyncHalResponseRenderer buildAsyncRenderer() {

    applyDefaultsBeforeBuilding();

    HalApiTypeSupport typeSupport = getEffectiveTypeSupport();

    AsyncHalResourceRenderer resourceRenderer = new AsyncHalResourceRendererImpl(metrics, typeSupport);

    ExceptionStatusAndLoggingStrategy exceptionStrategy = getEffectiveExceptionStrategy();

    return new AsyncHalResponseRendererImpl(resourceRenderer, metrics, exceptionStrategy, typeSupport, rhymeDocsSupport);
  }

  HalApiClient buildApiClient() {

    applyDefaultsBeforeBuilding();

    HalApiTypeSupport effectiveTypeSupport = getEffectiveTypeSupport();

    return new HalApiClientImpl(resourceLoader, metrics, effectiveTypeSupport);
  }

  Rhyme buildRhyme(String incomingRequestUri) {

    return new Rhyme() {

      private final HalApiClient client = buildApiClient();

      private final AsyncHalResponseRenderer renderer = buildAsyncRenderer();

      private final VndErrorResponseRenderer errorRenderer = VndErrorResponseRenderer.create(getEffectiveExceptionStrategy());

      @Override
      public <T> T getRemoteResource(String uri, Class<T> halApiInterface) {

        return client.getRemoteResource(uri, halApiInterface);
      }

      @Override
      public void setResponseMaxAge(Duration duration) {

        metrics.setResponseMaxAge(duration);
      }

      @Override
      public Single<HalResponse> renderResponse(LinkableResource resourceImpl) {

        return renderer.renderResponse(incomingRequestUri, resourceImpl);
      }

      @Override
      public HalResponse renderVndErrorResponse(Throwable error) {

        return errorRenderer.renderError(incomingRequestUri, null, error, metrics);
      }

      @Override
      public RequestMetricsStopwatch startStopwatch(Class clazz, Supplier<String> taskDescription) {

        return metrics.startStopwatch(clazz, taskDescription);
      }
    };
  }
}
