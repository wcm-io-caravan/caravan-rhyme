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
package io.wcm.caravan.rhyme.api.client;

import java.time.Clock;
import java.time.Duration;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HalResponseCache;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;
import io.wcm.caravan.rhyme.impl.client.HalResourceLoaderBuilderImpl;

@ProviderType
public interface HalResourceLoaderBuilder {

  public static HalResourceLoaderBuilder create() {
    return new HalResourceLoaderBuilderImpl();
  }

  HalResourceLoaderBuilder withCustomLoader(HalResourceLoader customLoader);

  HalResourceLoaderBuilder withCustomHttpClient(HttpClientSupport client);

  HalResourceLoaderBuilder withMemoryCache();

  HalResourceLoaderBuilder withMemoryCache(int maxNumItems, Duration timeToIdle);

  HalResourceLoaderBuilder withCustomCache(HalResponseCache cacheImplementation);

  HalResourceLoaderBuilder withCachingConfiguration(CachingConfiguration config);

  HalResourceLoaderBuilder withClock(Clock customClock);

  HalResourceLoader build();

}
