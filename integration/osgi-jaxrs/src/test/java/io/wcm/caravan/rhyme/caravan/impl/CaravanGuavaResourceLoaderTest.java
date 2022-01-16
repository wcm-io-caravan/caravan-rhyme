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
package io.wcm.caravan.rhyme.caravan.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.rhyme.api.client.CachingConfiguration;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

@ExtendWith(MockitoExtension.class)
public class CaravanGuavaResourceLoaderTest extends AbstractCaravanJsonResourceLoaderTest {

  private HalResourceLoader resourceLoader;

  private MutableClock clock = new MutableClock();

  @Override
  protected HalResourceLoader getResourceLoader() {
    return resourceLoader;
  }

  @BeforeEach
  void setUp() {

    resourceLoader = HalResourceLoaderBuilder.create()
        .withCustomHttpClient(new CaravanResilientHttpSupport(httpClient, EXTERNAL_SERVICE_ID))
        .withMemoryCache()
        .withCachingConfiguration(new CachingConfiguration() {

          @Override
          public int getDefaultMaxAge(Optional<Integer> statusCode) {
            return 0;
          }

          @Override
          public boolean isCachingOfHalApiClientExceptionsEnabled() {
            return false;
          }

        })
        .withClock(clock)
        .build();
  }

  void assertOkResponseWithMaxAge(Integer maxAge) {

    HalResponse response = getHalResponse();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMaxAge()).isEqualTo(maxAge);
  }


  @Test
  void getHalResource_should_use_cached_responses_if_not_stale() throws Exception {

    ObjectNode body = JsonNodeFactory.instance.objectNode();
    mockHttpResponse(200, body, Duration.ofSeconds(30));

    assertOkResponseWithMaxAge(30);

    clock.advance(Duration.ofSeconds(10));

    // the max-age is reduced by the time the clock has advanced
    assertOkResponseWithMaxAge(20);

    // there should be only one mocked http request
    verify(httpClient).execute(any());
  }

  @Test
  void getHalResource_should_discard_cached_responses_if_stale() throws Exception {

    ObjectNode body = JsonNodeFactory.instance.objectNode();
    mockHttpResponse(200, body, Duration.ofSeconds(30));

    assertOkResponseWithMaxAge(30);

    clock.advance(Duration.ofSeconds(40));

    // the max-age is taken from the second request
    assertOkResponseWithMaxAge(30);

    // there should be two mocked http requests
    verify(httpClient, times(2)).execute(any());
  }

  @Test
  void getHalResource_should_not_cache_responses_without_max_age() throws Exception {

    ObjectNode body = JsonNodeFactory.instance.objectNode();
    mockHttpResponse(200, body, null);

    assertOkResponseWithMaxAge(0);

    clock.advance(Duration.ofSeconds(70));

    assertOkResponseWithMaxAge(0);

    // there should be two mocked http requests
    verify(httpClient, times(2)).execute(any());
  }

  @Test
  void getHalResource_should_handle_invalid_json() throws Exception {

    CaravanHttpMockUtils.mockHttpResponse(httpClient, 200, "<body>Foo</body>", null);

    Throwable ex = catchThrowable(this::getHalResponse);

    assertThat(ex).isInstanceOf(HalApiClientException.class);
    assertThat(ex.getCause()).hasMessageContaining("but the body could not be successfully read and parsed");
  }

  static class MutableClock extends Clock {

    private Instant instant = Instant.ofEpochSecond(0);

    @Override
    public ZoneId getZone() {
      return ZoneId.systemDefault();
    }

    @Override
    public Instant instant() {
      return instant;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    public void advance(Duration duration) {
      instant = instant.plus(duration);
    }

  }
}
