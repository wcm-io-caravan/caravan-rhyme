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
package io.wcm.caravan.rhyme.api.spi;

import java.net.HttpURLConnection;
import java.net.URI;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.impl.client.http.HttpUrlConnectionSupport;

/**
 * As simpler callback-style SPI interface that you can implement instead of {@link HalResourceLoader}
 * if you want to enable {@link Rhyme} or {@link HalApiClient} instances
 * to fetch HTTP resources with a custom client library.
 * <p>
 * Your implementation should request the resource with the given URL and call the following methods *once* in this
 * order
 * <ul>
 * <li>{@link HttpClientCallback#onUrlModified(URI)} (optional)</li>
 * <li>{@link HttpClientCallback#onHeadersAvailable(int, java.util.Map)} (required)</li>
 * <li>{@link HttpClientCallback#onBodyAvailable(java.io.InputStream)} when the HTTP response body is ready to be read
 * </li>
 * </ul>
 * </p>
 * @see HttpUrlConnectionSupport a simple implementation using {@link HttpURLConnection}
 */
public interface HttpClientSupport {

  /**
   * @param uri
   * @param callback
   */
  void executeGetRequest(URI uri, HttpClientCallback callback);
}
