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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

public class HttpHalResourceLoader implements HalResourceLoader {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);

  private HttpClientSupport spi;

  private HttpHalResourceLoader(HttpClientSupport spi) {
    this.spi = spi;
  }

  public static HttpHalResourceLoader withClientImplementation(HttpClientSupport impl) {
    return new HttpHalResourceLoader(impl);
  }

  @Override
  public Single<HalResponse> getHalResource(String uri) {

    return Single.create(subscriber -> {

      Request request = new Request(uri, subscriber);

      request.get();
    });
  }

  class Request implements HttpClientCallback {

    private final SingleEmitter<HalResponse> subscriber;

    private final String originalUri;

    private volatile URI actualUri;

    private volatile HalResponse halResponse = new HalResponse();

    private AtomicBoolean done = new AtomicBoolean();

    Request(String uri, SingleEmitter<HalResponse> subscriber) {
      this.subscriber = subscriber;
      this.originalUri = uri;
    }

    private void emitHalResponse() {
      subscriber.onSuccess(halResponse);
    }

    private void emitHalApiClientExceptionWithCause(Exception cause) {
      if (done.compareAndSet(false, true)) {
        HalApiClientException ex = new HalApiClientException(halResponse, actualUri.toString(), cause);
        subscriber.onError(ex);
      }
    }

    public void get() {
      try {
        actualUri = URI.create(originalUri);

        spi.executeGetRequest(actualUri, this);
      }
      catch (RuntimeException ex) {
        emitHalApiClientExceptionWithCause(ex);
      }
    }

    @Override
    public void onUrlModified(URI uri) {

      actualUri = uri;
    }

    @Override
    public void onHeadersAvailable(int statusCode, Map<String, ? extends Collection<String>> headers) {

      updateStatusCode(statusCode);

      HttpHeadersParser parsedHeaders = new HttpHeadersParser(headers);

      parsedHeaders.getContentType()
          .ifPresent(this::updateContentType);

      parsedHeaders.getMaxAge()
          .ifPresent(this::updateMaxAge);
    }

    @Override
    public void onBodyAvailable(InputStream is) {

      boolean statusIsOk = halResponse.getStatus() == 200;

      try {
        JsonNode parsedJson = parseJson(is);

        updateBody(parsedJson);

        if (statusIsOk) {
          emitHalResponse();
        }
        else {
          emitHalApiClientExceptionWithCause(null);
        }
      }
      catch (RuntimeException ex) {
        if (statusIsOk) {
          updateStatusCode(null);
          emitHalApiClientExceptionWithCause(ex);
        }
        else {
          emitHalApiClientExceptionWithCause(null);
        }
      }
    }

    @Override
    public void onExceptionCaught(Exception ex) {

      emitHalApiClientExceptionWithCause(ex);
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
  }

  private JsonNode parseJson(InputStream is) {

    try (InputStream autoClosingStream = is) {
      return JSON_FACTORY.createParser(autoClosingStream).readValueAsTree();
    }
    catch (IOException | RuntimeException ex) {
      throw new RuntimeException("Failed to read or parse JSON response", ex);
    }
  }

}
