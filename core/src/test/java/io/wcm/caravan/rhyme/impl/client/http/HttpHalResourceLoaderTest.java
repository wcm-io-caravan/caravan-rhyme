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
package io.wcm.caravan.rhyme.impl.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.common.base.Charsets;

import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;


@ExtendWith(MockitoExtension.class)
public class HttpHalResourceLoaderTest {

  private static final String VALID_URI = "/foo";


  private HttpHalResourceLoader createLoader(HttpClientSupport client) {

    return HttpHalResourceLoader.withClientImplementation(client);
  }


  HalResponse executeGetRequestWith(String uri, HttpHalResourceLoader loader) {

    return loader.getHalResource(uri)
        .timeout(2, TimeUnit.SECONDS)
        .blockingGet();
  }

  private HalApiClientException loadResourceAndExpectClientException(HttpHalResourceLoader loader, String uri) {

    Throwable ex = catchThrowable(() -> executeGetRequestWith(uri, loader));

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class)
        .hasMessageStartingWith("HTTP request failed with status code");

    return (HalApiClientException)ex;
  }

  @Test
  public void should_parse_valid_URI() throws Exception {

    String validUri = "http://foo.bar";

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {

      assertThat(uri.toString())
          .isEqualTo(validUri);

      callback.onHeadersAvailable(200, Collections.emptyMap());
      callback.onBodyAvailable(createUtf8Stream("{}"));
    });

    executeGetRequestWith(validUri, loader);
  }

  @Test
  public void should_fail_to_parse_invalid_URI() throws Exception {

    String invalidUri = "ht%tp://fo.bar";

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {
      Assertions.fail("This should never be called if uri couldn't be parsed");
    });

    HalApiClientException ex = loadResourceAndExpectClientException(loader, invalidUri);

    assertThat(ex)
        .hasRootCauseInstanceOf(URISyntaxException.class);
  }

  @Test
  public void should_use_modified_URI_for_exceptions() throws Exception {

    URI baseUri = URI.create("http://foo.bar");

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {

      URI modifiedUri = baseUri.resolve(uri);

      callback.onUrlModified(modifiedUri);

      callback.onExceptionCaught(new RuntimeException("Something has gone wrong"));
    });

    HalApiClientException ex = loadResourceAndExpectClientException(loader, VALID_URI);

    assertThat(ex.getRequestUrl())
        .isEqualTo(baseUri.toString() + VALID_URI);
  }

  @Test
  public void should_wrap_runtime_exceptions_thrown_by_executeGetRequest() throws Exception {

    RuntimeException expectedCause = new RuntimeException("Something has gone wrong");

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {
      throw expectedCause;
    });

    HalApiClientException ex = loadResourceAndExpectClientException(loader, VALID_URI);

    assertThat(ex)
        .hasCauseReference(expectedCause);

    assertThat(ex.getStatusCode())
        .isNull();
  }

  @Test
  public void should_ignore_additional_calls_to_onExceptionThrown() throws Exception {

    RuntimeException firstException = new RuntimeException("Something has gone wrong");
    RuntimeException secondException = new RuntimeException("Even more has gone wrong");

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {
      callback.onExceptionCaught(firstException);
      callback.onExceptionCaught(secondException);
    });

    HalApiClientException ex = loadResourceAndExpectClientException(loader, VALID_URI);

    assertThat(ex)
        .hasCauseReference(firstException);

    assertThat(ex.getStatusCode())
        .isNull();
  }

  @Test
  public void should_fail_if_onBodyAvailable_is_called_before_onHeaderAvailable() throws Exception {

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {
      callback.onBodyAvailable(new ByteArrayInputStream(new byte[0]));
    });

    HalApiClientException ex = loadResourceAndExpectClientException(loader, VALID_URI);

    assertThat(ex)
        .hasCauseInstanceOf(HalApiDeveloperException.class);

    assertThat(ex.getCause())
        .hasMessage("onHeadersAvailable() should be called before onBodyAvailable()");
  }

  @Test
  public void should_emit_HalResponse_for_ok_json_response() throws Exception {

    String jsonString = "{\"foo\": 123}";

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {

      callback.onHeadersAvailable(200, Collections.emptyMap());
      callback.onBodyAvailable(createUtf8Stream(jsonString));
    });

    HalResponse response = executeGetRequestWith(VALID_URI, loader);

    assertThat(response.getStatus())
        .isEqualTo(200);

    JSONAssert.assertEquals(jsonString, response.getBody().getModel().toString(), true);
  }

  ByteArrayInputStream createUtf8Stream(String json) {
    return new ByteArrayInputStream(json.getBytes(Charsets.UTF_8));
  }

}