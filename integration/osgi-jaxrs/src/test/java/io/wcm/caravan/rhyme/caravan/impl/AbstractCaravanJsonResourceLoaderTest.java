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
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.apache.commons.io.Charsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import rx.Observable;

abstract class AbstractCaravanJsonResourceLoaderTest {

  private static final String REQUEST_URL = "/foo/bar";
  protected static final String EXTERNAL_SERVICE_ID = "/external/service/id";

  @Mock
  protected CaravanHttpClient httpClient;

  protected abstract HalResourceLoader getResourceLoader();

  protected void mockHttpResponse(int status, ObjectNode body, Duration maxAge) {

    CaravanHttpMockUtils.mockHttpResponse(httpClient, status, body, maxAge);
  }

  private void mockIllegalResponseRuntimeException(int status, String body) {

    CaravanHttpRequest request = new CaravanHttpRequestBuilder(EXTERNAL_SERVICE_ID).append(REQUEST_URL).build();

    IllegalResponseRuntimeException ex = new IllegalResponseRuntimeException(request, REQUEST_URL, status, body, "Failed");

    when(httpClient.execute(ArgumentMatchers.any()))
        .thenReturn(Observable.error(ex));
  }

  private void mockExceptionReadingBody() {

    CaravanHttpResponse response = new CaravanHttpResponseBuilder()
        .status(200)
        .reason("OK")
        // the null body triggers an exception when calling CaravanHttpResponse.body().asInputStream().
        .body(null, Charsets.US_ASCII)
        .build();

    when(httpClient.execute(ArgumentMatchers.any()))
        .thenReturn(Observable.just(response));
  }

  protected HalResponse getHalResponse() {
    return getResourceLoader().getHalResource(REQUEST_URL).blockingGet();
  }

  @Test
  void getHalResource_should_return_pipeline_output() {

    ObjectNode body = JsonNodeFactory.instance.objectNode().put("foo", "bar");

    mockHttpResponse(200, body, null);

    HalResponse response = getHalResponse();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getBody().getModel()).isEqualTo(body);
    assertThat(response.getUri()).isEqualTo(REQUEST_URL);
  }

  @Test
  void getHalResource_should_forward_max_age() {

    ObjectNode body = JsonNodeFactory.instance.objectNode();

    mockHttpResponse(200, body, Duration.ofSeconds(123));

    HalResponse response = getHalResponse();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMaxAge()).isEqualTo(123);
  }

  @Test
  void getHalResource_should_throw_HalApiClientException_for_404_responses() {

    ObjectNode body = JsonNodeFactory.instance.objectNode();

    mockHttpResponse(404, body, null);

    Throwable ex = catchThrowable(this::getHalResponse);

    assertThat(ex).isInstanceOf(HalApiClientException.class)
        .hasMessageContaining("has failed with status code 404");

    assertThat(((HalApiClientException)ex).getErrorResponse().getUri())
        .isEqualTo(REQUEST_URL);
  }

  @Test
  void getHalResource_should_throw_HalApiClientException_for_501_responses_without_body() {

    mockIllegalResponseRuntimeException(502, null);

    Throwable ex = catchThrowable(this::getHalResponse);

    assertThat(ex).isInstanceOf(HalApiClientException.class)
        .hasMessageContaining("has failed with status code 502");

    assertThat(((HalApiClientException)ex).getErrorResponse().getUri())
        .isEqualTo(REQUEST_URL);
  }

  @Test
  void getHalResource_should_throw_HalApiClientException_for_501_responses_with_json_body() {

    mockIllegalResponseRuntimeException(501, "{\"foo\": \"bar\"}");

    Throwable ex = catchThrowable(this::getHalResponse);

    assertThat(ex).isInstanceOf(HalApiClientException.class)
        .hasMessageContaining("has failed with status code 501");
  }

  @Test
  void getHalResource_should_throw_HalApiClientException_for_500_responses_with_non_json_body() {

    mockIllegalResponseRuntimeException(500, "foo");

    Throwable ex = catchThrowable(this::getHalResponse);

    assertThat(ex).isInstanceOf(HalApiClientException.class)
        .hasMessageContaining("has failed with status code 500");
  }

  @Test
  void getHalResource_should_throw_HalApiClientException_for_500_responses_with_status_code_in_body() {

    mockIllegalResponseRuntimeException(500, "500 Internal Server Error");

    Throwable ex = catchThrowable(this::getHalResponse);

    assertThat(ex).isInstanceOf(HalApiClientException.class)
        .hasMessageContaining("has failed with status code 500");

    assertThat(((HalApiClientException)ex).getErrorResponse().getBody())
        .isNull();
  }

  @Test
  void getHalResource_should_throw_HalApiClientException_for_unexpected_exceptions() {

    // trigger a runtime exception within JsonPipeline by having the http client return an unexpected empty observable
    when(httpClient.execute(ArgumentMatchers.any()))
        .thenReturn(Observable.empty());

    Throwable ex = catchThrowable(this::getHalResponse);

    assertThat(ex).isInstanceOf(HalApiClientException.class)
        .hasMessageContaining("has failed before a status code was available");

    assertThat(((HalApiClientException)ex).getErrorResponse().getUri())
        .isEqualTo(REQUEST_URL);
  }

  @Test
  void getHalResource_should_handle_fatal_errors() {

    when(httpClient.execute(ArgumentMatchers.any()))
        .thenReturn(Observable.error(new Error("Something really went wrong")));

    Throwable ex = catchThrowable(this::getHalResponse);

    assertThat(ex).isInstanceOf(HalApiClientException.class);
  }

  @Test
  void getHalResource_should_handle_errors_reading_body() {

    mockExceptionReadingBody();

    Throwable ex = catchThrowable(this::getHalResponse);

    assertThat(ex).isInstanceOf(HalApiClientException.class);
  }

}
