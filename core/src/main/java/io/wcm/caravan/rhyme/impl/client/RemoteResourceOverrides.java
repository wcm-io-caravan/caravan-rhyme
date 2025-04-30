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
package io.wcm.caravan.rhyme.impl.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;

/**
 * Provides alternative implementations of {@link HalApiInterface}s that should be returned when
 * {@link HalApiClient#getRemoteResource(String, Class)} or {@link Rhyme#getRemoteResource(String, Class)}
 * is called for a specific entry point URI and interface combination.
 */
public class RemoteResourceOverrides {

  private final Map<String, Function<RequestMetricsCollector, ?>> factoryMap = new HashMap<>();

  private String constructKey(Class<?> halApiInterface, String entryPointUrl) {

    return halApiInterface.getName() + "@" + entryPointUrl;
  }

  /**
   * Defines an override for a specific combination of entry point URI and interface
   * @param <T> the {@link HalApiInterface} type
   * @param entryPointUri the URI for which the override will be used
   * @param halApiInterface an interface defining the HAL API for that URI
   * @param factorFunc a function that will return a proxy, stub or server-side implementation of the given interface
   */
  public <T> void add(String entryPointUri, Class<T> halApiInterface, Function<RequestMetricsCollector, T> factorFunc) {

    String key = constructKey(halApiInterface, entryPointUri);

    factoryMap.put(key, factorFunc);
  }

  <T> Optional<T> get(Class<T> halApiInterface, String entryPointUrl, RequestMetricsCollector metrics) {

    String key = constructKey(halApiInterface, entryPointUrl);

    if (!factoryMap.containsKey(key)) {
      return Optional.empty();
    }

    @SuppressWarnings("unchecked") // this cast should be safe because the add method ensures that the function type matches the interface
    Function<RequestMetricsCollector, T> function = (Function<RequestMetricsCollector, T>)factoryMap.get(key);

    return Optional.of(function.apply(metrics));
  }
}
