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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.reha.api.client.HalApiClient;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.spi.JsonResourceLoader;
import io.wcm.caravan.reha.caravan.api.CaravanHalApiClient;

@Component
public class CaravanHalApiClientImpl implements CaravanHalApiClient {

  @Reference
  private CaravanHttpClient httpClient;

  @Reference
  private JsonPipelineFactory pipelineFactory;

  @Override
  public <T> T getEntryPoint(String serviceId, String uri, Class<T> halApiInterface, RequestMetricsCollector metrics) {

    // an implementation based on Caravan JsonPipeline caching
    // JsonResourceLoader jsonLoader = new CaravanJsonPipelineResourceLoader(pipelineFactory, serviceId);

    // an alternative implementation with less performance overhead (that doesn't use JsonPipeline,
    // and only has a simple guava based cache that does not evict any item)
    // only use this for now if you want to investigate performance issues with the HalApiClient
    JsonResourceLoader jsonLoader = new CaravanGuavaJsonResourceLoader(httpClient, serviceId);

    HalApiClient client = HalApiClient.create(jsonLoader, metrics);

    return client.getEntryPoint(uri, halApiInterface);
  }

}
