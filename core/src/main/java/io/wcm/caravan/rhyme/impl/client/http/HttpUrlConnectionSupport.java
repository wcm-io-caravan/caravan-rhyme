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
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;


/**
 * A default implementation of {@link HttpClientSupport} that is used in factory methods
 * that allow requesting resources without providing a more sophisticated implementation.
 * It's using {@link HttpURLConnection} without any configuration options.
 * @see HalResourceLoader#create()
 * @see HalApiClient#create()
 * @see RhymeBuilder#create()
 */
public class HttpUrlConnectionSupport implements HttpClientSupport {

  @Override
  public void executeGetRequest(URI uri, HttpClientCallback callback) {

    try {
      HttpURLConnection connection = (HttpURLConnection)uri.toURL().openConnection();

      int statusCode = connection.getResponseCode();
      Map<String, List<String>> headers = connection.getHeaderFields();

      callback.onHeadersAvailable(statusCode, headers);

      if (statusCode == 200) {
        callback.onBodyAvailable(connection.getInputStream());
      }
      else {
        callback.onBodyAvailable(connection.getErrorStream());
      }
    }
    catch (IOException ex) {
      callback.onExceptionCaught(ex);
    }
  }
}
