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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.common.collect.LinkedHashMultimap;

import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;


/**
 * An HTTP client implementation that is using the synchronous Apache HTTP client to execute requests.
 * @see HalResourceLoader#create(HttpClientSupport)
 * @see HalResourceLoaderBuilder#withCustomHttpClient(HttpClientSupport)
 */
public class ApacheBlockingHttpSupport implements HttpClientSupport {

  private final CloseableHttpClient httpClient;

  private final URI baseUri;

  /**
   * Default constructor that can be used if all URIs are fully qualified and requests
   * can be executed with a default HTTP client created with {@link HttpClientBuilder}
   */
  public ApacheBlockingHttpSupport() {
    this((URI)null);
  }

  /**
   * Allows to provide a customised {@link CloseableHttpClient} instance to be used for all requests.
   * @param client to use for all requests
   */
  public ApacheBlockingHttpSupport(CloseableHttpClient client) {
    this.httpClient = client;
    this.baseUri = null;
  }

  /**
   * An alternative constructor that will resolve all paths against the given Base URI
   * @param baseUri a fully qualified base URI
   */
  public ApacheBlockingHttpSupport(URI baseUri) {
    this.httpClient = HttpClientBuilder.create().build();
    this.baseUri = baseUri;
  }

  static Map<String, Collection<String>> getHeaders(HttpResponse response) {

    LinkedHashMultimap<String, String> headers = LinkedHashMultimap.create();

    for (Header header : response.getAllHeaders()) {
      headers.put(header.getName(), header.getValue());
    }

    return headers.asMap();
  }

  @Override
  public void executeGetRequest(URI uri, HttpClientCallback callback) {

    URI requestUri = uri;
    if (baseUri != null) {
      requestUri = baseUri.resolve(uri);
      callback.onUrlModified(requestUri);
    }

    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUri))) {

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
}
