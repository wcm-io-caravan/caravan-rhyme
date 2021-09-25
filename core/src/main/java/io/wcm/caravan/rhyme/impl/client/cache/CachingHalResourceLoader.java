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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.client.CachingConfiguration;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HalResponseCache;

public class CachingHalResourceLoader implements HalResourceLoader {

  private static final Logger log = LoggerFactory.getLogger(CachingHalResourceLoader.class);

  private final HalResourceLoader upstream;

  private final HalResponseCache cache;

  private final CachingConfiguration configuration;

  private final Clock clock;

  public CachingHalResourceLoader(HalResourceLoader upstream, HalResponseCache cache, CachingConfiguration configuration,
      Clock clock) {
    log.info("{} was created using {} as a cache backend. "
        + "If you are seeing this log message frequently, then you are not re-using your HalResourceLoader instance properly.",
        getClass().getSimpleName(), cache.getClass());
    this.upstream = upstream;
    this.cache = cache;
    this.configuration = configuration;
    this.clock = clock;
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
        .map(CachedResponse::getResponseWithAdjustedMaxAge)
        .flatMap(this::throwExceptionForErrorStatusCodes)
        .doOnSuccess(response -> log.debug("A fresh response for {} was as found in {} with remaining max-age of {}",
            uri, cache.getClass().getSimpleName(), response.getMaxAge()));
  }

  private Single<HalResponse> loadFromUpstreamAndStoreInCache(String uri) {

    return upstream.getHalResource(uri)
        .map(this::updateResponseWithTimestampAndDefaultMaxAge)
        .doOnSuccess(this::storeInCache)
        .doOnError(this::handleResourceLoaderException);
  }

  private Maybe<HalResponse> throwExceptionForErrorStatusCodes(HalResponse response) {

    if (response.getStatus() != null && response.getStatus() < 400) {
      return Maybe.just(response);
    }

    String msg = "An error response with status code " + response.getStatus() + " from a previous request was found in cache,"
        + " which will still be used for the next " + response.getMaxAge() + " seconds.";

    log.info(msg);

    RuntimeException cause = new RuntimeException(msg);

    return Maybe.error(new HalApiClientException(response, cause));
  }

  private HalResponse updateResponseWithTimestampAndDefaultMaxAge(HalResponse response) {

    HalResponse updatedResponse = response
        .withTimestamp(clock.instant());

    if (updatedResponse.getMaxAge() != null) {
      return updatedResponse;
    }

    Optional<Integer> status = Optional.ofNullable(updatedResponse.getStatus());
    int maxAge = configuration.getDefaultMaxAge(status);
    return updatedResponse.withMaxAge(maxAge);
  }

  private void storeInCache(HalResponse response) {

    if (response.getMaxAge() > 0) {
      log.debug("Response for {} is being stored in {} with max-age={} seconds", response.getUri(), cache.getClass().getSimpleName(), response.getMaxAge());

      cache.store(response);
    }
  }

  private void handleResourceLoaderException(Throwable ex) {

    if (!(ex instanceof HalApiClientException) || !configuration.isCachingOfHalApiClientExceptionsEnabled()) {
      return;
    }

    HalApiClientException hace = (HalApiClientException)ex;

    HalResponse updatedResponse = updateResponseWithTimestampAndDefaultMaxAge(hace.getErrorResponse());

    storeInCache(updatedResponse);
  }

  class CachedResponse {

    private final HalResponse response;

    private CachedResponse(HalResponse response) {
      this.response = response;
    }

    private int getSecondsInCache() {

      Duration cachedFor = Duration.between(response.getTimestamp(), clock.instant());

      return (int)cachedFor.getSeconds();
    }

    boolean isFresh() {

      int secondsInCache = getSecondsInCache();
      boolean fresh = secondsInCache < response.getMaxAge();

      if (!fresh) {
        log.debug("A response found in cache was considered stale, because it was stored {} seconds ago", secondsInCache);
      }

      return fresh;
    }

    HalResponse getResponseWithAdjustedMaxAge() {

      int newMaxAge = Math.max(0, response.getMaxAge() - getSecondsInCache());
      return response.withMaxAge(newMaxAge);
    }
  }

  HalResourceLoader getUpstream() {
    return upstream;
  }

  HalResponseCache getCache() {
    return cache;
  }

  CachingConfiguration getConfiguration() {
    return configuration;
  }

  Clock getClock() {
    return clock;
  }
}
