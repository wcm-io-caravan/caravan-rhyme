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

import java.net.URI;

import org.osgi.annotation.versioning.ConsumerType;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.impl.client.http.HttpUrlConnectionSupport;

/**
 * A simpler callback-style SPI interface that you can implement instead of {@link HalResourceLoader}
 * if you want to enable {@link Rhyme} or {@link HalApiClient} instances
 * to fetch HTTP resources with a custom client library.
 * You can then use {@link HalResourceLoaderBuilder#withCustomHttpClient(HttpClientSupport)} to build
 * to {@link HalResourceLoader} that uses your implementation, and adds the additional response parsing and error
 * handling.
 * @see HalResourceLoader
 * @see HalResourceLoaderBuilder
 * @see HttpUrlConnectionSupport
 */
@ConsumerType
public interface HttpClientSupport {

  /**
   * Starts executing a HTTP GET request for the given URL (either synchronously or asynchronously) and
   * calls the call the following methods *once* in this order:
   * <ul>
   * <li>{@link HttpClientCallback#onUrlModified(URI)} (optional)</li>
   * <li>{@link HttpClientCallback#onHeadersAvailable(int, java.util.Map)} (required)</li>
   * <li>{@link HttpClientCallback#onBodyAvailable(java.io.InputStream)} (required)</li>
   * </ul>
   * If any exception is thrown that prevents starting or completing the request, you must call
   * {@link HttpClientCallback#onExceptionCaught(Throwable)} once (and do not call any further methods).
   * @param uri the URI of the resource to load. This is usually a fully qualified HTTP(S) URL,but it could be any
   *          URI (depending on how the entry point was loaded and which kind of links are represented in the upstream
   *          resources)
   * @param callback that handles the response when it is available
   */
  void executeGetRequest(URI uri, HttpClientCallback callback);
}
