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
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;


public class UrlConnectionImplementation implements HttpClientImplementation {

  @Override
  public void executeRequest(URI uri, HttpClientCallback callback) {

    try {
      HttpURLConnection connection = (HttpURLConnection)uri.toURL().openConnection();

      int statusCode = connection.getResponseCode();
      Map<String, List<String>> headers = connection.getHeaderFields();

      callback.onHeadersAvailable(statusCode, headers);

      if (statusCode == HttpStatus.SC_OK) {
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
