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

import java.util.Optional;

import io.wcm.caravan.rhyme.api.client.CachingConfiguration;


public class DefaultCachingConfiguration implements CachingConfiguration {

  @Override
  public int getDefaultMaxAge(Optional<Integer> statusCode) {

    return statusCode
        .map(status -> 60)
        .orElse(0);
  }

  @Override
  public boolean isCachingOfHalApiClientExceptionsEnabled() {

    return false;
  }
}
