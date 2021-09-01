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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

import com.google.common.collect.LinkedHashMultimap;

import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

class ApacheAsyncHttpSupport implements HttpClientSupport {

  private final CloseableHttpAsyncClient httpClient = HttpAsyncClientBuilder.create().build();

  private final URI baseUri;

  public ApacheAsyncHttpSupport() {
    this(null);
  }

  public ApacheAsyncHttpSupport(URI baseUri) {
    this.baseUri = baseUri;
    this.httpClient.start();
  }

  @Override
  public void executeGetRequest(URI uri, HttpClientCallback callback) {

    URI requestUri = uri;
    if (baseUri != null) {
      baseUri.resolve(uri);
      callback.onUrlModified(requestUri);
    }

    httpClient.execute(new HttpGet(requestUri), new FutureCallback<HttpResponse>() {

      @Override
      public void failed(Exception ex) {
        callback.onExceptionCaught(ex);
      }

      @Override
      public void completed(HttpResponse response) {

        try {
          int statusCode = response.getStatusLine().getStatusCode();

          Map<String, Collection<String>> headers = getHeaders(response);

          callback.onHeadersAvailable(statusCode, headers);

          InputStream is = response.getEntity().getContent();

          callback.onBodyAvailable(is);
        }
        catch (IOException | RuntimeException ex) {
          callback.onExceptionCaught(ex);
        }
      }

      private Map<String, Collection<String>> getHeaders(HttpResponse httpResponse) {

        LinkedHashMultimap<String, String> headers = LinkedHashMultimap.create();

        for (Header header : httpResponse.getAllHeaders()) {
          headers.put(header.getName(), header.getValue());
        }

        return headers.asMap();
      }

      @Override
      public void cancelled() {
        // no need to implement anything here
      }
    });
  }

}
