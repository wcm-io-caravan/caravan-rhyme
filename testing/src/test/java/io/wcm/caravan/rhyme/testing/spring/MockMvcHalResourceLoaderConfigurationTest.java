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
package io.wcm.caravan.rhyme.testing.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

@SpringBootTest
@Import(MockMvcHalResourceLoaderConfiguration.class)
public class MockMvcHalResourceLoaderConfigurationTest {

  @Autowired
  private HalResourceLoader loader;

  @Test
  public void should_retrieve_status_and_body_for_200_response() throws Exception {

    HalResponse response = loader.getHalResource("/").blockingGet();

    assertThat(response.getStatus())
        .isEqualTo(200);

    assertThat(response.getBody().getModel().path("foo").asText())
        .isEqualTo("bar");

    assertThat(response.getContentType())
        .isEqualTo("application/hal+json");
  }

  @Test
  public void should_retrieve_status_and_body_for_404_response() throws Exception {

    Throwable ex = catchThrowable(() -> loader.getHalResource("/404").blockingGet());

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class);

    HalApiClientException hace = (HalApiClientException)ex;

    assertThat(hace.getStatusCode())
        .isEqualTo(404);

    assertThat(hace.getErrorResponse().getBody().getModel().path("foo").asText())
        .isEqualTo("bar");
  }

  @Test
  public void should_handle_exception() throws Exception {

    Throwable ex = catchThrowable(() -> loader.getHalResource("/500").blockingGet());

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class);

    HalApiClientException hace = (HalApiClientException)ex;

    assertThat(hace.getStatusCode())
        .isNull();

    assertThat(hace)
        .hasRootCauseInstanceOf(RuntimeException.class)
        .hasRootCauseMessage("Something has gone seriously wrong");
  }


  @Configuration
  static class Config {

    @Bean
    TestController testController() {
      return new TestController();
    }
  }

  @RestController
  static class TestController {

    private static final String FOO_BAR_JSON = JsonNodeFactory.instance.objectNode()
        .put("foo", "bar")
        .toString();

    @GetMapping(path = "/", produces = "application/hal+json")
    public ResponseEntity<String> root() {

      return ResponseEntity
          .ok(FOO_BAR_JSON);
    }

    @GetMapping("/404")
    public ResponseEntity<String> notFound() {

      return ResponseEntity
          .status(404)
          .body(FOO_BAR_JSON);
    }

    @GetMapping("/500")
    public ResponseEntity<String> serverError() {

      throw new RuntimeException("Something has gone seriously wrong");
    }
  }

}
