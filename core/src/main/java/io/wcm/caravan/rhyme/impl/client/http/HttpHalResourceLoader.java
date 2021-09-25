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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

/**
 * An adapter class that implements {@link HalResourceLoader} using a {@link HttpClientSupport} implementation,
 * and takes care that {@link #getHalResource} fails with a {@link HalApiClientException} containing status code and
 * other error information if anything goes wrong
 * @see HalResourceLoader
 * @see HttpClientSupport
 */
public class HttpHalResourceLoader implements HalResourceLoader {

  private static final Logger log = LoggerFactory.getLogger(HttpHalResourceLoader.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);

  private final HttpClientSupport client;

  private HttpHalResourceLoader(HttpClientSupport client) {
    this.client = client;
  }

  public static HttpHalResourceLoader withClientImplementation(HttpClientSupport client) {

    return new HttpHalResourceLoader(client);
  }

  @Override
  public Single<HalResponse> getHalResource(String uri) {

    return Single.create(subscriber -> {

      HttpClientCallbackImpl callback = new HttpClientCallbackImpl(uri, subscriber);

      callback.executeRequestAndWaitForCallbacks();
    });
  }

  private class HttpClientCallbackImpl implements HttpClientCallback {

    private final Stopwatch stopwatch = Stopwatch.createStarted();

    private final SingleEmitter<HalResponse> subscriber;
    private final AtomicBoolean responseOrErrorWasEmitted = new AtomicBoolean();

    private final String originalUri;
    private volatile URI actualUri;

    private volatile HttpHeadersParser parsedHeaders;

    private volatile HalResponse halResponse = new HalResponse();

    HttpClientCallbackImpl(String uri, SingleEmitter<HalResponse> subscriber) {
      this.subscriber = subscriber;
      this.originalUri = uri;
    }

    void executeRequestAndWaitForCallbacks() {
      try {
        updateUri(originalUri);

        actualUri = URI.create(originalUri);

        client.executeGetRequest(actualUri, this);
      }
      catch (RuntimeException ex) {

        emitHalApiClientExceptionWithCause(ex);
      }
    }

    private void updateUri(String uri) {
      halResponse = halResponse.withUri(uri);
    }

    private void updateStatusCode(Integer statusCode) {
      halResponse = halResponse.withStatus(statusCode);
    }

    private void updateContentType(String contentType) {
      halResponse = halResponse.withContentType(contentType);
    }

    private void updateMaxAge(Integer maxAge) {
      halResponse = halResponse.withMaxAge(maxAge);
    }

    private void updateBody(JsonNode parsedJson) {
      halResponse = halResponse.withBody(parsedJson);
    }

    private void emitHalResponse() {

      if (responseOrErrorWasEmitted.compareAndSet(false, true)) {
        log.debug("HTTP response from {} was retrieved in {}", actualUri, stopwatch);

        subscriber.onSuccess(halResponse);
      }
      else {
        log.warn("#onBodyAvailable wa called more than once, or after #onExceptionCaught was called ", new RuntimeException());
      }
    }

    private void emitHalApiClientExceptionWithCause(Throwable cause) {

      if (responseOrErrorWasEmitted.compareAndSet(false, true)) {

        HalApiClientException ex = new HalApiClientException(halResponse, cause);

        log.debug("HTTP request to {} failed with status {} after {}", halResponse.getUri(), halResponse.getStatus(), stopwatch);

        subscriber.onError(ex);
      }
      else {
        log.warn("An exception was caught after the response (or a previous exception) was already emitted ", cause);
      }
    }

    @Override
    public void onUrlModified(URI uri) {

      actualUri = uri;

      updateUri(uri.toString());
    }

    @Override
    public void onHeadersAvailable(int statusCode, Map<String, ? extends Collection<String>> headers) {

      // HttpURLConnection will sometimes return statusCode -1 for failed requests, which we don't want to forward
      updateStatusCode(statusCode > 0 ? statusCode : null);

      parsedHeaders = new HttpHeadersParser(headers);

      parsedHeaders.getContentType()
          .ifPresent(this::updateContentType);

      parsedHeaders.getMaxAge()
          .ifPresent(this::updateMaxAge);
    }

    @Override
    public void onBodyAvailable(InputStream is) {

      if (parsedHeaders == null) {
        throw new HalApiDeveloperException("onHeadersAvailable() should be called before onBodyAvailable()");
      }

      Integer status = halResponse.getStatus();
      String msgPrefix = "An HTTP response with status code " + status + " was retrieved, ";
      boolean statusIsOk = status != null && status == 200;

      try {
        // we try to parse the JSON and include it in the HalResponse even when the request failed
        JsonNode parsedJson = parseJson(is);

        updateBody(parsedJson);

        if (statusIsOk) {
          // this is the only case where the response was successfully retrieved and parsed
          emitHalResponse();
        }
        else {
          // the response code indicates that the request was *not* successful even through the response could be parsed
          String msg;
          if (StringUtils.equals(halResponse.getContentType(), VndErrorResponseRenderer.CONTENT_TYPE)) {
            msg = msgPrefix + "which contains a vnd.error body with the following server-side error details";
          }
          else {
            msg = msgPrefix + "and a JSON body that may contain further information is present";
          }
          emitHalApiClientExceptionWithCause(new HttpClientSupportException(msg));
        }
      }
      catch (RuntimeException ex) {
        if (statusIsOk) {
          String msg = msgPrefix + "but the body could not be successfully read and parsed as a JSON document";

          emitHalApiClientExceptionWithCause(new HttpClientSupportException(msg, ex));
        }
        else {
          // if JSON parsing failed for a non-ok response, we don't include that exception as a root cause,
          // as we cannot expected the body to be JSON all the time, and don't want confusing information
          // in the stack trace
          String msg = msgPrefix + "and no JSON body with further information was found";
          emitHalApiClientExceptionWithCause(new HttpClientSupportException(msg));
        }
      }
    }

    @Override
    public void onExceptionCaught(Throwable ex) {

      emitHalApiClientExceptionWithCause(ex);
    }

  }

  private static JsonNode parseJson(InputStream is) {

    try (InputStream autoClosingStream = is) {
      return JSON_FACTORY.createParser(autoClosingStream).readValueAsTree();
    }
    catch (JsonProcessingException ex) {
      throw new HttpClientSupportException("The response body was read completely, but it's not valid JSON.", ex);
    }
    catch (IOException ex) {
      throw new HttpClientSupportException("The response body could not be read completely from the input stream", ex);
    }
  }

  static class HttpClientSupportException extends RuntimeException {

    private static final long serialVersionUID = -1086569417284265963L;

    HttpClientSupportException(String msg) {
      super(msg);
    }

    HttpClientSupportException(String msg, Throwable cause) {
      super(msg, cause);
    }
  }

  public HttpClientSupport getClient() {
    return client;
  }

}
