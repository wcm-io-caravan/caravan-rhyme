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
package io.wcm.caravan.rhyme.spring.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.testing.client.AbstractHalResourceLoaderTest;

/**
 * Runs a set of tests for the {@link WebClientSupport} based HalResourceLoader against a Wiremock server
 */
class WebClientSupportTest extends AbstractHalResourceLoaderTest {

  @Override
  protected HalResourceLoader createLoaderUnderTest() {

    WebClient webClient = WebClient.create();
    return HalResourceLoader.create(new WebClientSupport(uri -> webClient));
  }

  // after upgrading to Spring Boot 2.5.8, WebClient is handling a few edge cases differently,
  // so for now we are overriding the test with updated expectations

  @Override
  @Test
  public void cause_should_be_present_in_HalApiClientException_for_for_network_errors() {

    HalApiClientException ex = loadResourceAndExpectClientException(UNKNOWN_HOST_URL);

    assertThat(ex)
        .hasCauseInstanceOf(WebClientRequestException.class);
  }

  @Override
  @Test
  public void cause_should_be_present_in_HalApiClientException_for_for_ssl_errors() {

    HalApiClientException ex = loadResourceAndExpectClientException(sslTestUrl);

    assertThat(ex)
        .hasRootCauseMessage("unable to find valid certification path to requested target");
  }
}
