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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.reactivex.rxjava3.core.Maybe;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResponseCache;

public class GuavaCacheImplementation implements HalResponseCache {

  private final int maxNumItems;
  private final Duration timeToIdle;

  private final Cache<String, HalResponse> cache;

  public GuavaCacheImplementation(int maxNumItems, Duration timeToIdle) {

    this.maxNumItems = maxNumItems;
    this.timeToIdle = timeToIdle;

    this.cache = CacheBuilder.newBuilder()
        .maximumSize(maxNumItems)
        .expireAfterAccess(timeToIdle.getSeconds(), TimeUnit.SECONDS)
        .build();
  }

  @Override
  public Maybe<HalResponse> load(String uri) {

    HalResponse entry = cache.getIfPresent(uri);

    return entry != null ? Maybe.just(entry) : Maybe.empty();
  }

  @Override
  public void store(HalResponse response) {

    cache.put(response.getUri(), response);
  }

  int getMaxNumItems() {
    return maxNumItems;
  }

  Duration getTimeToIdle() {
    return timeToIdle;
  }

}
