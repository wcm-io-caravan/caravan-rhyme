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
package io.wcm.caravan.rhyme.aem.impl.client;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import com.google.common.collect.LinkedHashMultimap;

import io.wcm.caravan.commons.httpclient.HttpClientFactory;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

public class ApacheHttpClientFactorySupport implements HttpClientSupport {

  private final HttpClientFactory clientFactory;

  public ApacheHttpClientFactorySupport(HttpClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  private Map<String, Collection<String>> getHeaders(HttpResponse response) {

    LinkedHashMultimap<String, String> headers = LinkedHashMultimap.create();

    for (Header header : response.getAllHeaders()) {
      headers.put(header.getName(), header.getValue());
    }

    return headers.asMap();
  }

  @Override
  public void executeGetRequest(URI uri, HttpClientCallback callback) {

    CloseableHttpClient httpClient = clientFactory.getCloseable(uri.toString());

    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(uri))) {

      callback.onHeadersAvailable(response.getStatusLine().getStatusCode(), getHeaders(response));

      callback.onBodyAvailable(response.getEntity().getContent());
    }
    catch (IOException | RuntimeException ex) {
      callback.onExceptionCaught(ex);
    }
  }
}
