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

import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mercateo.test.clock.TestClock;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.MockClientTestSupport.SubscriberCounter;

@ExtendWith(MockitoExtension.class)
public class CachingHalResourceLoaderTest {

  private static final String URI = "/";

  private TestClock clock = TestClock.fixed(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

  private final MockClientTestSupport upstream = ClientTestSupport.withMocking();

  @Mock
  private CachingConfiguration config;

  private HalResponseCache cache;

  private CachingHalResourceLoader loader;

  private ObjectNode body = createBody("foo");


  @BeforeEach
  void setUp() {
    cache = new GuavaCacheImplementation();
    loader = new CachingHalResourceLoader(upstream.getMockJsonLoader(), cache, config, clock);
  }

  @Test
  public void should_return_upstream_response_on_cache_miss() throws Exception {

    mockOkResponseWithTextAndMaxAge("foo", 60);
    loadResponseAndAssertTextIs("foo");
  }

  @Test
  public void should_return_fresh_cached_response() throws Exception {

    int maxAge = 60;

    mockOkResponseWithTextAndMaxAge("original", maxAge);
    loadResponseAndAssertTextIs("original");

    mockOkResponseWithTextAndMaxAge("updated", maxAge);
    loadResponseAndAssertTextIs("original");
  }

  @Test
  public void should_not_used_stale_cached_response() throws Exception {

    int maxAge = 60;

    mockOkResponseWithTextAndMaxAge("original", maxAge);
    loadResponseAndAssertTextIs("original");

    clock.fastForward(Duration.ofSeconds(maxAge));

    mockOkResponseWithTextAndMaxAge("updated", maxAge);
    loadResponseAndAssertTextIs("updated");
  }

  @Test
  public void should_not_used_cached_response_if_max_age_is_zero() throws Exception {

    int maxAge = 0;

    mockOkResponseWithTextAndMaxAge("original", maxAge);
    loadResponseAndAssertTextIs("original");

    mockOkResponseWithTextAndMaxAge("updated", maxAge);
    loadResponseAndAssertTextIs("updated");
  }

  @Test
  public void should_cache_and_use_default_max_age_if_null_in_response() throws Exception {

    Integer responseMaxAge = null;
    int defaultMaxAge = 25;

    when(config.getDefaultMaxAge(any()))
        .thenReturn(defaultMaxAge);

    mockOkResponseWithTextAndMaxAge("original", responseMaxAge);
    HalResponse firstResponse = loadResourceWithCaching();

    assertThat(firstResponse.getMaxAge())
        .isEqualTo(defaultMaxAge);

    mockOkResponseWithTextAndMaxAge("updated", responseMaxAge);
    HalResponse cachedResponse = loadResponseAndAssertTextIs("original");

    assertThat(cachedResponse.getMaxAge())
        .isEqualTo(defaultMaxAge);
  }

  @Test
  public void should_update_max_age_of_cached_response() throws Exception {

    mockOkResponseWithTextAndMaxAge("original", 60);
    loadResourceWithCaching();

    clock.fastForward(Duration.ofSeconds(20));

    HalResponse response = loadResponseAndAssertTextIs("original");
    assertThat(response.getMaxAge())
        .isEqualTo(40);
  }

  private HalResponse loadResponseAndAssertTextIs(String text) {

    HalResponse response = loadResourceWithCaching();

    return assertThatBodyHasText(text, response);
  }

  private HalResponse assertThatBodyHasText(String text, HalResponse response) {

    assertThat(response.getBody())
        .isNotNull();

    assertThat(response.getBody().getModel().path("text").asText())
        .isEqualTo(text);

    return response;
  }


  HalResponse loadResourceWithCaching() {

    return loader.getHalResource(URI).blockingGet();
  }

  void assertThatBodyIsEqual(HalResponse usedResponse) {
    assertThat(usedResponse.getBody().getModel())
        .isEqualTo(body);
  }

  private SubscriberCounter mockOkResponseWithTextAndMaxAge(String text, Integer maxAge) {

    HalResponse response = createResponseWithTextAndMaxAge(text, maxAge);

    return upstream.mockResponseWithSingle(URI, Single.just(response));
  }

  HalResponse createResponseWithTextAndMaxAge(String text, Integer maxAge) {

    return new HalResponse()
        .withBody(createBody(text))
        .withStatus(SC_OK)
        .withMaxAge(maxAge);
  }


  private static ObjectNode createBody(String text) {
    return JsonNodeFactory.instance.objectNode()
        .put("text", text);
  }

}
