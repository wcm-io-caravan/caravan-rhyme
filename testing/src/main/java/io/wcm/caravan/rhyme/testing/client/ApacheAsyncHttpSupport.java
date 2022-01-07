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
package io.wcm.caravan.rhyme.testing.client;

import static io.wcm.caravan.rhyme.testing.client.ApacheBlockingHttpSupport.getHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;
import io.wcm.caravan.rhyme.tooling.annotations.ExcludeFromJacocoGeneratedReport;

/**
 * An HTTP client implementation that is using the asynchronous Apache HTTP client to execute requests.
 * @see HalResourceLoader#create(HttpClientSupport)
 * @see HalResourceLoaderBuilder#withCustomHttpClient(HttpClientSupport)
 */
public class ApacheAsyncHttpSupport implements HttpClientSupport {

  private final CloseableHttpAsyncClient httpClient;

  private final URI baseUri;

  /**
   * Default constructor that can be used if all URIs are fully qualified and requests
   * can be executed with a default HTTP client created with {@link HttpClientBuilder}
   */
  public ApacheAsyncHttpSupport() {
    this((URI)null);
  }

  /**
   * Allows to provide a customised {@link CloseableHttpAsyncClient} instance to be used for all requests.
   * @param client to use for all requests
   */
  public ApacheAsyncHttpSupport(CloseableHttpAsyncClient client) {
    this.httpClient = client;
    this.baseUri = null;
    if (!httpClient.isRunning()) {
      httpClient.start();
    }
  }

  /**
   * An alternative constructor that will resolve all paths against the given Base URI
   * @param baseUri a fully qualified base URI
   */
  public ApacheAsyncHttpSupport(URI baseUri) {
    this.httpClient = HttpAsyncClientBuilder.create().build();
    this.baseUri = baseUri;
    this.httpClient.start();
  }

  @Override
  public void executeGetRequest(URI uri, HttpClientCallback callback) {

    URI requestUri = uri;
    if (baseUri != null) {
      requestUri = baseUri.resolve(uri);
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

      @Override
      @ExcludeFromJacocoGeneratedReport
      public void cancelled() {
        // no need to implement anything here, since the code cannot cancel the request
      }
    });
  }

}
