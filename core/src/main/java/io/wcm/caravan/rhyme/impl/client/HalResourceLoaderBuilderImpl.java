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
package io.wcm.caravan.rhyme.impl.client;

import java.time.Clock;
import java.time.Duration;

import io.wcm.caravan.rhyme.api.client.CachingConfiguration;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HalResponseCache;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;
import io.wcm.caravan.rhyme.impl.client.cache.CachingHalResourceLoader;
import io.wcm.caravan.rhyme.impl.client.cache.DefaultCachingConfiguration;
import io.wcm.caravan.rhyme.impl.client.cache.GuavaCacheImplementation;
import io.wcm.caravan.rhyme.impl.client.http.HttpHalResourceLoader;
import io.wcm.caravan.rhyme.impl.client.http.HttpUrlConnectionSupport;

public class HalResourceLoaderBuilderImpl implements HalResourceLoaderBuilder {

  private final HalResourceLoader loader;
  private final CachingConfiguration cachingConfig;
  private final HalResponseCache cache;
  private final Clock clock;

  public HalResourceLoaderBuilderImpl() {
    this.loader = HttpHalResourceLoader.withClientImplementation(new HttpUrlConnectionSupport());
    this.cachingConfig = new DefaultCachingConfiguration();
    this.cache = null;
    this.clock = Clock.systemUTC();
  }

  private HalResourceLoaderBuilderImpl(HalResourceLoader loader, CachingConfiguration cachingConfig, HalResponseCache cache, Clock clock) {
    this.loader = loader;
    this.cachingConfig = cachingConfig;
    this.cache = cache;
    this.clock = clock;
  }

  @Override
  public HalResourceLoaderBuilder withCustomLoader(HalResourceLoader customLoader) {

    return new HalResourceLoaderBuilderImpl(customLoader, cachingConfig, cache, clock);
  }

  @Override
  public HalResourceLoaderBuilder withCustomHttpClient(HttpClientSupport client) {

    HttpHalResourceLoader customLoader = HttpHalResourceLoader.withClientImplementation(client);

    return new HalResourceLoaderBuilderImpl(customLoader, cachingConfig, cache, clock);
  }

  @Override
  public HalResourceLoaderBuilder withMemoryCache() {

    return withMemoryCache(10000, Duration.ofDays(1));
  }

  @Override
  public HalResourceLoaderBuilder withMemoryCache(int maxNumItems, Duration timeToIdle) {

    GuavaCacheImplementation guava = new GuavaCacheImplementation(maxNumItems, timeToIdle);

    return new HalResourceLoaderBuilderImpl(loader, cachingConfig, guava, clock);
  }

  @Override
  public HalResourceLoaderBuilder withCustomCache(HalResponseCache cacheImplementation) {

    return new HalResourceLoaderBuilderImpl(loader, cachingConfig, cacheImplementation, clock);
  }

  @Override
  public HalResourceLoaderBuilder withCachingConfiguration(CachingConfiguration config) {

    return new HalResourceLoaderBuilderImpl(loader, config, cache, clock);
  }

  @Override
  public HalResourceLoaderBuilder withClock(Clock customClock) {

    return new HalResourceLoaderBuilderImpl(loader, cachingConfig, cache, customClock);
  }

  @Override
  public HalResourceLoader build() {

    if (cache != null) {
      return new CachingHalResourceLoader(loader, cache, cachingConfig, clock);
    }

    return loader;
  }

}
