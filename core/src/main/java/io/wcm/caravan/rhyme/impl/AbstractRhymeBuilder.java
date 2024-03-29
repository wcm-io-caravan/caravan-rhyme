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

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalApiClientBuilder;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.server.HalResponseRendererBuilder;
import io.wcm.caravan.rhyme.api.server.RhymeMetadataConfiguration;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;
import io.wcm.caravan.rhyme.impl.client.HalApiClientImpl;
import io.wcm.caravan.rhyme.impl.client.RemoteResourceOverrides;
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
 * @param <I> the interface that the subclass is implementing
 */
abstract class AbstractRhymeBuilder<I> {

  private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
      .disable(FAIL_ON_UNKNOWN_PROPERTIES);

  private final Stopwatch stopwatch = Stopwatch.createStarted();

  private HalResourceLoader resourceLoader;

  private RequestMetricsCollector metrics;

  private final List<ExceptionStatusAndLoggingStrategy> exceptionStrategies = new ArrayList<>();

  private final List<HalApiTypeSupport> typeSupports = new ArrayList<>();

  private RhymeDocsSupport rhymeDocsSupport;

  private RhymeMetadataConfiguration metadataConfiguration;

  private ObjectMapper objectMapper;

  private final RemoteResourceOverrides resourceOverrides = new RemoteResourceOverrides();

  protected boolean wasUsedToBuild;

  @SuppressWarnings("unchecked")
  public I withResourceLoader(HalResourceLoader resourceLoader) {

    this.resourceLoader = resourceLoader;
    return (I)this;
  }

  @SuppressWarnings("unchecked")
  public I withMetrics(RequestMetricsCollector sharedMetrics) {

    this.metrics = sharedMetrics;
    return (I)this;
  }

  @SuppressWarnings("unchecked")
  public I withRhymeDocsSupport(RhymeDocsSupport rhymeDocsSupport) {

    this.rhymeDocsSupport = rhymeDocsSupport;
    return (I)this;
  }

  @SuppressWarnings("unchecked")
  public I withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport) {

    if (additionalTypeSupport != null) {
      typeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    }
    return (I)this;
  }

  @SuppressWarnings("unchecked")
  public I withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport) {

    if (additionalTypeSupport != null) {
      typeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    }
    return (I)this;
  }

  @SuppressWarnings("unchecked")
  public I withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy) {

    exceptionStrategies.add(customStrategy);
    return (I)this;
  }

  @SuppressWarnings("unchecked")
  public I withMetadataConfiguration(RhymeMetadataConfiguration configuration) {

    metadataConfiguration = configuration;
    return (I)this;
  }

  @SuppressWarnings("unchecked")
  public I withObjectMapper(ObjectMapper customMapper) {

    objectMapper = customMapper;
    return (I)this;
  }

  @SuppressWarnings("unchecked")
  public <T> I withRemoteResourceOverride(String entryPointUri, Class<T> halApiInterface, Function<RequestMetricsCollector, T> factoryFunc) {

    resourceOverrides.add(entryPointUri, halApiInterface, factoryFunc);
    return (I)this;
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

    if (metadataConfiguration == null) {
      metadataConfiguration = new RhymeMetadataConfiguration() {
        // use default implementations from interface only
      };
    }

    if (metrics == null) {
      if (metadataConfiguration.isMetadataGenerationEnabled()) {
        metrics = RequestMetricsCollector.create();
      }
      else {
        metrics = RequestMetricsCollector.createEssentialCollector();
      }
    }

    if (objectMapper == null) {
      objectMapper = DEFAULT_OBJECT_MAPPER;
    }
  }

  AsyncHalResponseRenderer buildAsyncRenderer() {

    applyDefaultsBeforeBuilding();

    HalApiTypeSupport typeSupport = getEffectiveTypeSupport();

    AsyncHalResourceRenderer resourceRenderer = new AsyncHalResourceRendererImpl(metrics, typeSupport, objectMapper);

    ExceptionStatusAndLoggingStrategy exceptionStrategy = getEffectiveExceptionStrategy();

    return new AsyncHalResponseRendererImpl(resourceRenderer, metrics, exceptionStrategy, typeSupport, rhymeDocsSupport);
  }

  HalApiClient buildApiClient() {

    applyDefaultsBeforeBuilding();

    HalApiTypeSupport effectiveTypeSupport = getEffectiveTypeSupport();

    return new HalApiClientImpl(resourceLoader, metrics, effectiveTypeSupport, objectMapper, resourceOverrides);
  }

  @SuppressWarnings("deprecation")
  Rhyme buildRhyme(String incomingRequestUri) {

    HalApiClient client = buildApiClient();

    AsyncHalResponseRenderer renderer = buildAsyncRenderer();

    VndErrorResponseRenderer errorRenderer = VndErrorResponseRenderer.create(getEffectiveExceptionStrategy());

    RhymeImpl impl = new RhymeImpl(incomingRequestUri, client, renderer, errorRenderer, metrics);

    metrics.onMethodInvocationFinished(AsyncHalResponseRenderer.class, "building Rhyme instance", stopwatch.elapsed(TimeUnit.MICROSECONDS));

    return impl;
  }
}
