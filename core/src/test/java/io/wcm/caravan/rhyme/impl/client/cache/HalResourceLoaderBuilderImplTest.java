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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.wcm.caravan.rhyme.api.client.CachingConfiguration;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HalResponseCache;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;
import io.wcm.caravan.rhyme.impl.client.http.HttpHalResourceLoader;
import io.wcm.caravan.rhyme.testing.TestClock;

@ExtendWith(MockitoExtension.class)
class HalResourceLoaderBuilderImplTest {

  @Mock
  private HalResourceLoader mockLoader;

  @Mock
  private HttpClientSupport httpClient;

  @Mock
  private HalResponseCache cache;

  @Mock
  private CachingConfiguration config;

  private Clock clock = TestClock.fixed(Instant.EPOCH, ZoneId.systemDefault());


  @Test
  void withExistingLoader_should_use_custom_loader() throws Exception {

    HalResourceLoader loader = HalResourceLoaderBuilder.create()
        .withExistingLoader(mockLoader)
        .build();

    assertThat(loader)
        .isSameAs(mockLoader);
  }

  @Test
  void withCustomHttpClient_should_build_HttpHalResourceLoader() throws Exception {

    HalResourceLoader loader = HalResourceLoaderBuilder.create()
        .withCustomHttpClient(httpClient)
        .build();

    assertThat(loader)
        .isInstanceOf(HttpHalResourceLoader.class);

    assertThat(((HttpHalResourceLoader)loader).getClient())
        .isSameAs(httpClient);
  }


  @Test
  void withMemoryCache_should_build_CachingHttpHalResourceLoader_with_default_settings() throws Exception {

    HalResourceLoader loader = HalResourceLoaderBuilder.create()
        .withMemoryCache()
        .build();

    assertThat(loader)
        .isInstanceOf(CachingHalResourceLoader.class);

    CachingHalResourceLoader cachingLoader = (CachingHalResourceLoader)loader;

    assertThat(cachingLoader.getUpstream())
        .isInstanceOf(HttpHalResourceLoader.class);

    assertThat(cachingLoader.getConfiguration())
        .isInstanceOf(DefaultCachingConfiguration.class);

    assertThat(cachingLoader.getCache())
        .isInstanceOf(GuavaCacheImplementation.class);

    GuavaCacheImplementation impl = (GuavaCacheImplementation)cachingLoader.getCache();
    assertThat(impl.getMaxNumItems())
        .isEqualTo(10000);
    assertThat(impl.getTimeToIdle())
        .isEqualTo(Duration.ofDays(1));
  }

  @Test
  void withMemoryCache_should_build_CachingHttpHalResourceLoader_with_custom_settings() throws Exception {

    int maxNumItems = 100;
    Duration timeToIdle = Duration.ofMinutes(5);

    HalResourceLoader loader = HalResourceLoaderBuilder.create()
        .withMemoryCache(maxNumItems, timeToIdle)
        .build();

    assertThat(loader)
        .isInstanceOf(CachingHalResourceLoader.class);

    CachingHalResourceLoader cachingLoader = (CachingHalResourceLoader)loader;

    assertThat(cachingLoader.getUpstream())
        .isInstanceOf(HttpHalResourceLoader.class);

    assertThat(cachingLoader.getConfiguration())
        .isInstanceOf(DefaultCachingConfiguration.class);

    assertThat(cachingLoader.getCache())
        .isInstanceOf(GuavaCacheImplementation.class);

    GuavaCacheImplementation impl = (GuavaCacheImplementation)cachingLoader.getCache();
    assertThat(impl.getMaxNumItems())
        .isEqualTo(maxNumItems);
    assertThat(impl.getTimeToIdle())
        .isEqualTo(timeToIdle);
  }

  @Test
  void withCustomHttpClient_can_be_called_beforewithMemoryCache() throws Exception {

    HalResourceLoader loader = HalResourceLoaderBuilder.create()
        .withCustomHttpClient(httpClient)
        .withMemoryCache()
        .build();

    assertThat(loader)
        .isInstanceOf(CachingHalResourceLoader.class);
    CachingHalResourceLoader cachingLoader = (CachingHalResourceLoader)loader;

    assertThat(cachingLoader.getUpstream())
        .isInstanceOf(HttpHalResourceLoader.class);
    HttpHalResourceLoader httpLoader = (HttpHalResourceLoader)cachingLoader.getUpstream();

    assertThat(httpLoader.getClient())
        .isSameAs(httpClient);
  }

  @Test
  void withCustomHttpClient_can_be_called_after_withMemoryCache() throws Exception {

    HalResourceLoader loader = HalResourceLoaderBuilder.create()
        .withMemoryCache()
        .withCustomHttpClient(httpClient)
        .build();

    assertThat(loader)
        .isInstanceOf(CachingHalResourceLoader.class);
    CachingHalResourceLoader cachingLoader = (CachingHalResourceLoader)loader;

    assertThat(cachingLoader.getUpstream())
        .isInstanceOf(HttpHalResourceLoader.class);
    HttpHalResourceLoader httpLoader = (HttpHalResourceLoader)cachingLoader.getUpstream();

    assertThat(httpLoader.getClient())
        .isSameAs(httpClient);
  }

  @Test
  void withCustomCache_should_build_CachingHttpHalResourceLoader() throws Exception {

    HalResourceLoader loader = HalResourceLoaderBuilder.create()
        .withCustomCache(cache)
        .build();

    assertThat(loader)
        .isInstanceOf(CachingHalResourceLoader.class);

    CachingHalResourceLoader cachingLoader = (CachingHalResourceLoader)loader;

    assertThat(cachingLoader.getUpstream())
        .isInstanceOf(HttpHalResourceLoader.class);

    assertThat(cachingLoader.getConfiguration())
        .isInstanceOf(DefaultCachingConfiguration.class);

    assertThat(cachingLoader.getCache())
        .isSameAs(cache);
  }

  @Test
  void withCachingConfiguration_can_be_called_after_withMemoryCache() throws Exception {

    HalResourceLoader loader = HalResourceLoaderBuilder.create()
        .withMemoryCache()
        .withCachingConfiguration(config)
        .build();

    assertThat(loader)
        .isInstanceOf(CachingHalResourceLoader.class);

    CachingHalResourceLoader cachingLoader = (CachingHalResourceLoader)loader;

    assertThat(cachingLoader.getConfiguration())
        .isSameAs(config);
  }

  @Test
  void withCachingConfiguration_can_be_called_before_withMemoryCache() throws Exception {

    HalResourceLoader loader = HalResourceLoaderBuilder.create()
        .withCachingConfiguration(config)
        .withMemoryCache()
        .build();

    assertThat(loader)
        .isInstanceOf(CachingHalResourceLoader.class);

    CachingHalResourceLoader cachingLoader = (CachingHalResourceLoader)loader;

    assertThat(cachingLoader.getConfiguration())
        .isSameAs(config);
  }


  @Test
  void withCachingConfiguration_should_fail_if_no_cache_implementation_was_specified() throws Exception {

    Throwable ex = catchThrowable(() -> HalResourceLoaderBuilder.create()
        .withCachingConfiguration(config)
        .build());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("You have only provided a CachingConfiguration");
  }

  @Test
  void withClock_should_allow_to_specify_custom_clock() throws Exception {

    HalResourceLoader loader = HalResourceLoaderBuilder.create()
        .withMemoryCache()
        .withClock(clock)
        .build();

    assertThat(loader)
        .isInstanceOf(CachingHalResourceLoader.class);

    CachingHalResourceLoader cachingLoader = (CachingHalResourceLoader)loader;

    assertThat(cachingLoader.getClock())
        .isSameAs(clock);
  }

}
