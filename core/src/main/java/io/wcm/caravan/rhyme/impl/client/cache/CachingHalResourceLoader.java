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
package io.wcm.caravan.rhyme.impl.client.cache;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

public class CachingHalResourceLoader implements HalResourceLoader {

  private final HalResourceLoader upstream;

  private final HalResponseCache cache;

  private final CachingConfiguration configuration;

  private final Clock clock;

  public CachingHalResourceLoader(HalResourceLoader upstream, HalResponseCache cache, CachingConfiguration configuration,
      Clock clock) {
    this.upstream = upstream;
    this.cache = cache;
    this.configuration = configuration;
    this.clock = clock;
  }

  public static HalResourceLoader createWithGuavaCache(HalResourceLoader upstream) {
    return new CachingHalResourceLoader(upstream, new GuavaCacheImplementation(), new CachingConfiguration() {
    }, Clock.systemUTC());
  }

  @Override
  public Single<HalResponse> getHalResource(String uri) {

    return loadFromCache(uri)
        .switchIfEmpty(loadFromUpstreamAndStoreInCache(uri));
  }

  private Maybe<HalResponse> loadFromCache(String uri) {

    return cache.load(uri)
        .map(CachedResponse::new)
        .filter(CachedResponse::isFresh)
        .map(CachedResponse::getResponseWithAdjustedMaxAge);
  }

  private Single<HalResponse> loadFromUpstreamAndStoreInCache(String uri) {

    return upstream.getHalResource(uri)
        .map(this::updateResponse)
        .doOnSuccess(entry -> storeInCache(uri, entry));
  }

  private HalResponse updateResponse(HalResponse response) {

    HalResponse updatedResponse = response
        .withInstant(clock.instant());

    if (response.getMaxAge() == null) {
      Optional<Integer> status = Optional.ofNullable(response.getStatus());
      int maxAge = configuration.getDefaultMaxAge(status);
      updatedResponse = updatedResponse.withMaxAge(maxAge);
    }

    return updatedResponse;
  }

  private void storeInCache(String uri, HalResponse response) {

    if (response.getMaxAge() > 0) {
      cache.store(uri, response);
    }
  }

  class CachedResponse {

    private final HalResponse entry;

    private CachedResponse(HalResponse response) {
      this.entry = response;
    }

    private int getSecondsInCache() {

      Duration cachedFor = Duration.between(entry.getInstant(), clock.instant());

      return (int)cachedFor.getSeconds();
    }

    boolean isFresh() {

      return getSecondsInCache() < entry.getMaxAge();
    }

    HalResponse getResponseWithAdjustedMaxAge() {

      int newMaxAge = Math.max(0, entry.getMaxAge() - getSecondsInCache());
      return entry.withMaxAge(newMaxAge);
    }
  }

}
