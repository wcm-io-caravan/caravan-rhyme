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
package io.wcm.caravan.ryhme.testing.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

class HttpHalResourceLoader implements HalResourceLoader {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);

  private HttpLoadingImplementation spi;

  public HttpHalResourceLoader(HttpLoadingImplementation spi) {
    this.spi = spi;
  }

  @Override
  public Single<HalResponse> getHalResource(String uri) {

    return Single.create(subscriber -> {

      Request request = new Request(uri, subscriber);

      request.get();
    });
  }

  interface ResponseCallback {

    void onUrlModified(URI uri);

    void onHeadersAvailable(int statusCode, Map<String, Collection<String>> contentType);

    void onBodyAvailable(InputStream is);

    void onExceptionCaught(Exception ex);
  }

  class Request implements HttpHalResourceLoader.ResponseCallback {

    private final SingleEmitter<HalResponse> subscriber;

    private final String originalUri;

    private volatile URI actualUri;

    private volatile HalResponse halResponse = new HalResponse();

    private AtomicBoolean done = new AtomicBoolean();

    Request(String uri, SingleEmitter<HalResponse> subscriber) {
      this.subscriber = subscriber;
      this.originalUri = uri;
    }

    public void get() {
      try {
        actualUri = URI.create(originalUri);

        spi.executeRequest(actualUri, this);
      }
      catch (RuntimeException ex) {
        onExceptionCaught(ex);
      }
    }

    @Override
    public void onUrlModified(URI uri) {

      actualUri = uri;
    }

    @Override
    public void onHeadersAvailable(int statusCode, Map<String, Collection<String>> headers) {

      halResponse = halResponse
          .withStatus(statusCode);

      findHeader("content-type", headers)
          .ifPresent(contentType -> halResponse = halResponse.withContentType(contentType));

      findHeader("cache-control", headers)
          .map(CacheControlUtil::parseMaxAge)
          .ifPresent(maxAge -> halResponse = halResponse.withMaxAge(maxAge));
    }

    private Optional<String> findHeader(String name, Map<String, Collection<String>> headers) {

      return headers.entrySet().stream()
          .filter(entry -> StringUtils.equalsIgnoreCase(name, entry.getKey()))
          .flatMap(entry -> entry.getValue().stream())
          .findFirst();
    }

    @Override
    public void onBodyAvailable(InputStream is) {

      boolean ok = halResponse.getStatus() == 200;

      try {
        JsonNode parsedJson = parseJson(is);

        halResponse = halResponse
            .withBody(parsedJson);

        if (ok) {
          subscriber.onSuccess(halResponse);
        }
        else {
          onExceptionCaught(null);
        }
      }
      catch (RuntimeException ex) {
        if (ok) {
          halResponse = halResponse.withStatus(null);
          onExceptionCaught(ex);
        }
        else {
          onExceptionCaught(null);
        }
      }
    }

    @Override
    public void onExceptionCaught(Exception cause) {

      if (done.compareAndSet(false, true)) {
        HalApiClientException ex = new HalApiClientException(halResponse, actualUri.toString(), cause);
        subscriber.onError(ex);
      }
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
