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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Charsets;

import hu.akarnokd.rxjava3.interop.RxJavaInterop;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.IllegalResponseRuntimeException;
import io.wcm.caravan.io.http.request.CaravanHttpRequest;
import io.wcm.caravan.io.http.request.CaravanHttpRequestBuilder;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

public class CaravanResilientHttpSupport implements HttpClientSupport {

  private final String serviceId;
  private final CaravanHttpClient client;

  CaravanResilientHttpSupport(CaravanHttpClient client, String serviceId) {
    this.serviceId = serviceId;
    this.client = client;
  }

  @Override
  public void executeGetRequest(URI uri, HttpClientCallback callback) {

    CaravanHttpRequest request = createRequest(uri);

    executeRequest(request)
        .subscribe(
            response -> processResponse(callback, response),
            ex -> handleException(callback, ex));
  }

  private CaravanHttpRequest createRequest(URI uri) {

    CaravanHttpRequestBuilder requestBuilder = new CaravanHttpRequestBuilder(serviceId);

    requestBuilder.append(uri.toString());

    return requestBuilder.build();
  }

  private Single<CaravanHttpResponse> executeRequest(CaravanHttpRequest request) {

    return RxJavaInterop.toV3Single(client.execute(request).toSingle());
  }

  void processResponse(HttpClientCallback callback, CaravanHttpResponse response) {

    try {
      callback.onHeadersAvailable(response.status(), response.headers().asMap());

      callback.onBodyAvailable(response.body().asInputStream());
    }
    catch (IOException | RuntimeException ex) {
      callback.onExceptionCaught(ex);
    }
  }

  void handleException(HttpClientCallback callback, Throwable ex) throws Throwable {

    if (ex instanceof IllegalResponseRuntimeException) {
      IllegalResponseRuntimeException irre = (IllegalResponseRuntimeException)ex;

      callback.onHeadersAvailable(irre.getResponseStatusCode(), Collections.emptyMap());

      String body = StringUtils.defaultIfBlank(irre.getResponseBody(), "");
      InputStream bodyAsStream = IOUtils.toInputStream(body, Charsets.UTF_8);
      callback.onBodyAvailable(bodyAsStream);

      return;
    }

    callback.onExceptionCaught(ex);
  }
}
