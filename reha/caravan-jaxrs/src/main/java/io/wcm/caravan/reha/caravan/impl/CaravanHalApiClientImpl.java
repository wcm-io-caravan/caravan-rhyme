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
package io.wcm.caravan.reha.caravan.impl;

import java.util.concurrent.ExecutionException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.reha.api.client.HalApiClient;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.reha.api.spi.JsonResourceLoader;
import io.wcm.caravan.reha.caravan.api.CaravanHalApiClient;

/**
 * Implementation of the {@link CaravanHalApiClient} OSGi service that will use the
 * {@link CaravanJsonPipelineResourceLoader} for caching if the caravan JSON pipeline bundles are available at runtime.
 * Otherwise, it will fall back to using the {@link CaravanGuavaJsonResourceLoader})
 */
@Component
public class CaravanHalApiClientImpl implements CaravanHalApiClient {

  private LoadingCache<String, JsonResourceLoader> resourceLoaderCache;

  @Reference
  private CaravanHttpClient httpClient;

  @Reference(cardinality = ReferenceCardinality.OPTIONAL)
  private JsonPipelineFactory pipelineFactory;

  @Activate
  void setUp() {

    CacheLoader<String, JsonResourceLoader> cacheLoader = createCacheLoaderWithBestAvailableCache();

    resourceLoaderCache = CacheBuilder.newBuilder().build(cacheLoader);
  }

  private CacheLoader<String, JsonResourceLoader> createCacheLoaderWithBestAvailableCache() {

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

    JsonResourceLoader jsonLoader = getOrCreateJsonResourceLoader(serviceId);

    HalApiClient client = HalApiClient.create(jsonLoader, metrics);

    return client.getEntryPoint(uri, halApiInterface);
  }

  @SuppressWarnings("PMD.PreserveStackTrace")
  JsonResourceLoader getOrCreateJsonResourceLoader(String serviceId) {
    try {
      return resourceLoaderCache.get(serviceId);
    }
    catch (ExecutionException | UncheckedExecutionException ex) {
      throw new HalApiDeveloperException("Failed to find guava cache", ex.getCause());
    }
  }

  private final class GuavaCacheLoader extends CacheLoader<String, JsonResourceLoader> {

    @Override
    public JsonResourceLoader load(String serviceId) throws Exception {
      return new CaravanGuavaJsonResourceLoader(httpClient, serviceId);
    }
  }

  private final class JsonPipelineCacheLoader extends CacheLoader<String, JsonResourceLoader> {

    @Override
    public JsonResourceLoader load(String serviceId) throws Exception {
      return new CaravanJsonPipelineResourceLoader(pipelineFactory, serviceId);
    }
  }
}
