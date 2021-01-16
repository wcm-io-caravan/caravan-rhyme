/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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

import static org.mockito.Mockito.when;

import java.time.Duration;

import org.apache.commons.io.Charsets;
import org.mockito.ArgumentMatchers;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.io.http.response.CaravanHttpResponse;
import io.wcm.caravan.io.http.response.CaravanHttpResponseBuilder;
import rx.Observable;

public final class CaravanHttpMockUtils {

  private CaravanHttpMockUtils() {
    // static methods only
  }

  static void mockHttpResponse(CaravanHttpClient clientMock, int status, ObjectNode body, Duration maxAge) {

    String bodyString = body != null ? body.toString() : null;

    mockHttpResponse(clientMock, status, bodyString, maxAge);
  }

  static void mockHttpResponse(CaravanHttpClient clientMock, int status, String bodyString, Duration maxAge) {

    CaravanHttpResponseBuilder builder = new CaravanHttpResponseBuilder()
        .status(status)
        .reason("OK");

    if (bodyString != null) {
      builder.body(bodyString, Charsets.UTF_8);
    }

    if (maxAge != null) {
      builder.header("cache-control", "max-age=" + maxAge.getSeconds());
    }

    CaravanHttpResponse response = builder.build();

    when(clientMock.execute(ArgumentMatchers.any()))
        .thenReturn(Observable.just(response));
  }
}
