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
package io.wcm.caravan.rhyme.caravan.impl;

import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Clock;
import java.util.concurrent.ExecutionException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.caravan.api.CaravanHalApiClient;

/**
 * Implementation of the {@link CaravanHalApiClient} OSGi service that will use the
 * {@link CaravanJsonPipelineResourceLoader} for caching if the caravan JSON pipeline bundles are available at runtime.
 * Otherwise, it will fall back to using the {@link CaravanGuavaResourceLoader})
 */
@Component
public class CaravanHalApiClientImpl implements CaravanHalApiClient {

  private LoadingCache<String, HalResourceLoader> resourceLoaderCache;

  @Reference
  private CaravanHttpClient httpClient;

  @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
  private JsonPipelineFactory pipelineFactory;

  @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
  private HalApiReturnTypeSupport returnTypeSupport;

  @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
  private HalApiAnnotationSupport annotationSupport;


  @Activate
  void setUp() {

    CacheLoader<String, HalResourceLoader> cacheLoader = createCacheLoaderWithBestAvailableCache();

    resourceLoaderCache = CacheBuilder.newBuilder().build(cacheLoader);
  }

  private CacheLoader<String, HalResourceLoader> createCacheLoaderWithBestAvailableCache() {

    if (pipelineFactory != null) {
      // an implementation based on Caravan JsonPipeline caching, use it if it's available in the runtime
      return new JsonPipelineCacheLoader();
    }

    // an alternative implementation with less performance overhead (that doesn't use JsonPipeline,
    // and only has a simple guava based cache that does not evict any item)
    return new GuavaCacheLoader();
  }

  @Override
  public <T> T getEntryPoint(String serviceId, String uri, Class<T> halApiInterface, RequestMetricsCollector metrics) {

    HalResourceLoader halLoader = getOrCreateHalResourceLoader(serviceId);

    HalApiClient client = HalApiClient.create(halLoader, metrics, annotationSupport, returnTypeSupport);

    return client.getRemoteResource(uri, halApiInterface);
  }

  @SuppressWarnings("PMD.PreserveStackTrace")
  HalResourceLoader getOrCreateHalResourceLoader(String serviceId) {
    try {
      return resourceLoaderCache.get(serviceId);
    }
    catch (ExecutionException | UncheckedExecutionException ex) {
      throw new HalApiDeveloperException("Failed to create resource loader for serviceId " + serviceId, ex.getCause());
    }
  }

  private final class GuavaCacheLoader extends CacheLoader<String, HalResourceLoader> {

    @Override
    public HalResourceLoader load(String serviceId) throws Exception {
      return new CaravanGuavaResourceLoader(httpClient, serviceId, Clock.systemUTC());
    }
  }

  private final class JsonPipelineCacheLoader extends CacheLoader<String, HalResourceLoader> {

    @Override
    public HalResourceLoader load(String serviceId) throws Exception {
      return new CaravanJsonPipelineResourceLoader(pipelineFactory, serviceId);
    }
  }
}
