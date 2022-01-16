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

import java.util.Optional;

import org.junit.jupiter.api.Test;


public class DefaultCachingConfigurationTest {

  private final DefaultCachingConfiguration config = new DefaultCachingConfiguration();

  @Test
  void getDefaultMaxAge_should_return_0_if_no_status_code_present() throws Exception {

    int maxAge = config.getDefaultMaxAge(Optional.empty());

    assertThat(maxAge)
        .isEqualTo(0);
  }

  @Test
  void getDefaultMaxAge_should_return_60_if_status_code_present() throws Exception {

    int maxAge = config.getDefaultMaxAge(Optional.of(200));

    assertThat(maxAge)
        .isEqualTo(60);
  }

  @Test
  void testIsCachingOfHalApiClientExceptionsEnabled() throws Exception {

    boolean enabled = config.isCachingOfHalApiClientExceptionsEnabled();

    assertThat(enabled)
        .isFalse();
  }

}
