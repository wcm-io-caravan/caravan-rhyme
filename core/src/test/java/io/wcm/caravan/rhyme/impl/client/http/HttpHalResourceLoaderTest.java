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
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;

import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;


@ExtendWith(MockitoExtension.class)
class HttpHalResourceLoaderTest {

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
        .hasMessageStartingWith("HAL client request to ")
        .hasMessageContaining("has failed");

    return (HalApiClientException)ex;
  }

  @Test
  void should_parse_valid_URI() throws Exception {

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
  void should_fail_to_parse_invalid_URI() throws Exception {

    String invalidUri = "ht%tp://fo.bar";

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {
      Assertions.fail("This should never be called if uri couldn't be parsed");
    });

    HalApiClientException ex = loadResourceAndExpectClientException(loader, invalidUri);

    assertThat(ex)
        .hasRootCauseInstanceOf(URISyntaxException.class);
  }

  @Test
  void should_use_modified_URI_in_response() throws Exception {

    URI baseUri = URI.create("http://foo.bar");

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {

      URI modifiedUri = baseUri.resolve(uri);

      callback.onUrlModified(modifiedUri);
      callback.onHeadersAvailable(200, Collections.emptyMap());
      callback.onBodyAvailable(createUtf8Stream("{}"));
    });

    HalResponse response = executeGetRequestWith(VALID_URI, loader);

    assertThat(response.getUri())
        .isEqualTo("http://foo.bar/foo");
  }

  @Test
  void should_extract_content_type_header() throws Exception {

    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, ImmutableList.of("foo/bar"));

    HalResponse response = executeSuccessfullRequestWithReponseHeaders(headers);

    assertThat(response.getContentType())
        .isEqualTo("foo/bar");
  }

  @Test
  void should_handle_null_header_with_status_line_from_HttpURLConnection_getHeaderFields() throws Exception {

    // HttpURLConnection has an odd behaviour of putting the status line in the header map (using null as key)
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put(null, ImmutableList.of("HTTP/1.1 200 OK"));

    HalResponse response = executeSuccessfullRequestWithReponseHeaders(headers);

    assertThat(response.getStatus())
        .isEqualTo(200);
  }

  private HalResponse executeSuccessfullRequestWithReponseHeaders(Map<String, Collection<String>> headers) {

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {

      callback.onHeadersAvailable(200, headers);
      callback.onBodyAvailable(createUtf8Stream("{}"));
    });

    return executeGetRequestWith(VALID_URI, loader);
  }

  @Test
  void should_use_modified_URI_for_exceptions() throws Exception {

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
  void should_wrap_runtime_exceptions_thrown_by_executeGetRequest() throws Exception {

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
  void should_ignore_additional_calls_to_onExceptionThrown() throws Exception {

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
  void should_fail_if_onBodyAvailable_is_called_before_onHeaderAvailable() throws Exception {

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
  void should_ignore_if_onBodyAvailable_is_called_multiple_times() throws Exception {

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {
      callback.onHeadersAvailable(200, Collections.emptyMap());
      callback.onBodyAvailable(createUtf8Stream("{}"));
      callback.onBodyAvailable(createUtf8Stream("{}"));
    });

    HalResponse response = executeGetRequestWith(VALID_URI, loader);

    assertThat(response.getStatus())
        .isEqualTo(200);
  }

  @Test
  void should_ignore_if_onBodyAvailable_is_called_after_onExceptionCaught() throws Exception {

    RuntimeException cause = new RuntimeException("Something has failed");

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {
      callback.onExceptionCaught(cause);
      callback.onBodyAvailable(new ByteArrayInputStream(new byte[0]));
    });

    HalApiClientException ex = loadResourceAndExpectClientException(loader, VALID_URI);

    assertThat(ex)
        .hasCauseReference(cause);
  }


  @Test
  void should_emit_HalResponse_for_ok_json_response() throws Exception {

    String jsonString = "{\"foo\": 123}";

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {

      callback.onHeadersAvailable(200, Collections.emptyMap());
      callback.onBodyAvailable(createUtf8Stream(jsonString));
    });

    HalResponse response = executeGetRequestWith(VALID_URI, loader);

    assertThat(response.getUri())
        .isEqualTo(VALID_URI);

    assertThat(response.getStatus())
        .isEqualTo(200);

    JSONAssert.assertEquals(jsonString, response.getBody().getModel().toString(), true);
  }

  @Test
  void should_fail_for_empty_json_response() throws Exception {

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {

      callback.onHeadersAvailable(200, Collections.emptyMap());
      callback.onBodyAvailable(new ByteArrayInputStream(new byte[0]));
    });

    HalApiClientException ex = loadResourceAndExpectClientException(loader, VALID_URI);

    assertThat(ex)
        .hasMessageStartingWith("HAL client request to /foo has failed because the response body is malformed")
        .getCause()
        .hasMessageContaining("the body could not be successfully read and parsed as a JSON document")
        .getCause()
        .hasMessage("The response body was completely empty (or consisted only of whitespace)");
  }

  @Test
  void should_fail_for_invalid_json_response() throws Exception {

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {

      callback.onHeadersAvailable(200, Collections.emptyMap());
      callback.onBodyAvailable(createUtf8Stream("{"));
    });

    HalApiClientException ex = loadResourceAndExpectClientException(loader, VALID_URI);

    assertThat(ex)
        .hasMessageStartingWith("HAL client request to /foo has failed because the response body is malformed")
        .getCause()
        .hasMessageContaining("the body could not be successfully read and parsed as a JSON document")
        .getCause()
        .hasMessage("The response body was read completely, but it's not valid JSON.");
  }

  @Test
  void should_fail_if_body_input_stream_fails() throws Exception {

    InputStream is = Mockito.mock(InputStream.class);

    HttpHalResourceLoader loader = createLoader((uri, callback) -> {

      callback.onHeadersAvailable(200, Collections.emptyMap());
      callback.onBodyAvailable(is);
    });

    HalApiClientException ex = loadResourceAndExpectClientException(loader, VALID_URI);

    assertThat(ex)
        .hasMessageStartingWith("HAL client request to /foo has failed because the response body is malformed")
        .getCause()
        .hasMessageContaining("the body could not be successfully read and parsed as a JSON document")
        .getCause()
        .hasMessage("The response body could not be read completely from the input stream");
  }

  ByteArrayInputStream createUtf8Stream(String json) {
    return new ByteArrayInputStream(json.getBytes(Charsets.UTF_8));
  }

}
