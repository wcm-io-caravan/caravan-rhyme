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
package io.wcm.caravan.reha.caravan.api;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;

/**
 * An OSGi service that allows to request HAL+JSON resources for a specific upstream service using the
 * {@link CaravanHttpClient}
 */
@ProviderType
public interface CaravanHalApiClient {

  /**
   * Create a dynamic proxy for the entry point of an upstream service
   * @param <T> an interface annotated with {@link HalApiInterface}
   * @param serviceId the ribbon ID for the upstream service
   * @param uri the absolute path of the entry point
   * @param halApiInterface an interface annotated with {@link HalApiInterface}
   * @param metrics to collect information on all upstream resources that have been retrieved
   * @return a dynamic proxy instance of the provided {@link HalApiInterface}
   */
  <T> T getEntryPoint(String serviceId, String uri, Class<T> halApiInterface, RequestMetricsCollector metrics);
}
