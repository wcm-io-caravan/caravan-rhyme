/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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

import java.io.IOException;
import java.time.Clock;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import hu.akarnokd.rxjava3.interop.RxJavaInterop;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.JsonResourceLoader;


class CaravanGuavaJsonResourceLoader implements JsonResourceLoader {

  private static final JsonFactory JSON_FACTORY = new JsonFactory(new ObjectMapper());

  private final Cache<String, CacheEntry> cache = CacheBuilder.newBuilder().build();

  private final String serviceId;

  private final CaravanHttpClient client;

  private final Clock clock;

  CaravanGuavaJsonResourceLoader(CaravanHttpClient client, String serviceId, Clock clock) {
    this.serviceId = serviceId;
    this.client = client;
    this.clock = clock;
  }

  @Override
  public Single<HalResponse> loadJsonResource(String uri) {

    CacheEntry cached = cache.getIfPresent(uri);

    if (cached != null && !cached.isStale()) {
      return Single.just(cached.getResponseWithAdjustedMaxAge());
    }

    CaravanHttpRequest request = createRequest(uri);

    return executeRequest(request)
        .map(response -> parseAndCacheResponse(uri, request, response))
        .onErrorResumeNext(ex -> rethrowAsHalApiClientException(ex, uri));
  }

  private CaravanHttpRequest createRequest(String uri) {

    CaravanHttpRequestBuilder requestBuilder = new CaravanHttpRequestBuilder(serviceId);

    requestBuilder.append(uri);

    return requestBuilder.build();
  }

  private Single<CaravanHttpResponse> executeRequest(CaravanHttpRequest request) {

    return RxJavaInterop.toV3Single(client.execute(request).toSingle());
  }

  @SuppressWarnings("PMD.AvoidRethrowingException")
  private HalResponse parseAndCacheResponse(String uri, CaravanHttpRequest request, CaravanHttpResponse response) {
    try {

      int statusCode = response.status();
      String responseBody = response.body().asString();

      JsonNode jsonNode;
      Integer maxAge;
      if (statusCode >= 400) {
        jsonNode = parseResponseBodyAndIgnoreErrors(responseBody);
        maxAge = null;
      }
      else {
        jsonNode = parseResponseBody(responseBody);
        maxAge = parseMaxAge(response);
      }

      HalResponse halResponse = new HalResponse()
          .withStatus(statusCode)
          .withReason(response.reason())
          .withBody(jsonNode)
          .withMaxAge(maxAge);

      if (statusCode >= 400) {
        IllegalResponseRuntimeException cause = new IllegalResponseRuntimeException(request, uri, statusCode, responseBody,
            "Received " + statusCode + " response from " + uri);
        throw new HalApiClientException(halResponse, uri, cause);
      }

      if (maxAge != null) {
        cache.put(uri, new CacheEntry(halResponse));
      }

      return halResponse;

    }
    catch (HalApiClientException ex) {
      throw ex;
    }
    catch (JsonParseException ex) {
      throw new RuntimeException("Failed to parse HAL/JSON body from " + uri, ex);
    }
    catch (IOException ex) {
      throw new RuntimeException("Failed to transfer HAL/JSON body from " + uri, ex);
    }
    // CHECKSTYLE:OFF - yes, we want to catch and rethrow all runtime exceptions
    catch (RuntimeException ex) {
      throw new RuntimeException("Failed to process HAL/JSON response body from " + uri, ex);
    }
  }

  private JsonNode parseResponseBody(String responseBody) throws IOException, JsonParseException {
    return JSON_FACTORY.createParser(responseBody).readValueAsTree();
  }

  private Integer parseMaxAge(CaravanHttpResponse response) {

    Integer maxAge = null;
    String maxAgeString = response.getCacheControl().get("max-age");
    if (maxAgeString != null) {
      try {
        maxAge = Integer.parseInt(maxAgeString);
      }
      catch (NumberFormatException ex) {
        // ignore
      }
    }
    return maxAge;
  }

  private JsonNode parseResponseBodyAndIgnoreErrors(String responseBody) {
    JsonNode jsonNode = null;
    try {
      jsonNode = parseResponseBody(responseBody);
    }
    catch (Exception ex) {
      // ignore any exceptions when trying to parse the response body for 40x errors
    }
    return jsonNode;
  }

  private Single<HalResponse> rethrowAsHalApiClientException(Throwable ex, String uri) {

    if (ex instanceof HalApiClientException) {
      return Single.error(ex);
    }

    if (ex instanceof IllegalResponseRuntimeException) {
      IllegalResponseRuntimeException irre = ((IllegalResponseRuntimeException)ex);

      JsonNode body = parseResponseBodyAndIgnoreErrors(irre.getResponseBody());

      HalResponse jsonResponse = new HalResponse()
          .withStatus(irre.getResponseStatusCode())
          .withBody(body);

      return Single.error(new HalApiClientException(jsonResponse, uri, ex));
    }

    String message = "HTTP request for " + uri + " failed without a status code (e.g. because of timeout, configuration or networking issues)";
    return Single.error(new HalApiClientException(message, null, uri, ex));
  }

  class CacheEntry {

    private final long timeRetrieved = clock.millis();
    private final HalResponse response;

    CacheEntry(HalResponse response) {
      this.response = response;
    }

    private int getSecondsInCache() {
      return (int)(clock.millis() - timeRetrieved) / 1000;
    }

    boolean isStale() {
      if (response.getMaxAge() != null) {
        int secondsCached = getSecondsInCache();
        return secondsCached > response.getMaxAge();
      }
      return false;
    }

    HalResponse getResponseWithAdjustedMaxAge() {
      if (response.getMaxAge() == null) {
        return response;
      }
      int newMaxAge = Math.max(0, response.getMaxAge() - getSecondsInCache());
      return response.withMaxAge(newMaxAge);
    }
  }

}
