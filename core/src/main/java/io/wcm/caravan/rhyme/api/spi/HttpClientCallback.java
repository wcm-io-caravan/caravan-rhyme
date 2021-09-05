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

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;

/**
 * Callback interface used by {@link HttpClientSupport#executeGetRequest(URI, HttpClientCallback)}
 * to process a response that has been retrieved, and handle any errors according to the contract
 * of the {@link HalResourceLoader#getHalResource(String)} method
 * @see HttpClientSupport
 * @see HalResourceLoader
 */
public interface HttpClientCallback {

  /**
   * This should be called if your client had to modify the URL (e.g. add or replace the host
   * name) in order to execute the request. This will ensure that the URL that was actually used will appear
   * in any {@link HalApiClientException}s that are thrown by {@link HalResourceLoader#getHalResource(String)}
   * @param uri from which the resource was actually requested
   */
  void onUrlModified(URI uri);

  /**
   * This should be called as soon as the HTTP response headers are available. It will ensure that
   * {@link HalResponse#getStatus()}, {@link HalResponse#getContentType()} and {@link HalResponse#getMaxAge()} will
   * return the values from your response
   * @param statusCode HTTP status code from the response
   * @param headers a map with name and values of all HTTP headers in the response
   */
  void onHeadersAvailable(int statusCode, Map<String, ? extends Collection<String>> headers);

  /**
   * This *must* be called after {@link #onHeadersAvailable(int, Map)} and as soon as the response body is ready to be
   * read. It will ensure that {@link HalResponse#getBody()} will return the parsed HAL+JSON response.
   * <p>
   * If you don't call this method, then you *must* call {@link #onExceptionCaught(Throwable)} instead, so that
   * the framework knows that the request has actually been completed. You *should* call this method even if the
   * response
   * contained an error status code, so that the framework can try to parse the response body as JSON to retrieve
   * <a href="https://github.com/blongden/vnd.error">vnd.error</a>
   * information. Any errors trying to read from the stream or parse the JSON will be caught and make
   * {@link HalResourceLoader#getHalResource(String)} fail with a {@link HalApiClientException}
   * </p>
   * @param responseBodyStream the input stream that will be closed after the response body has been read
   */
  void onBodyAvailable(InputStream responseBodyStream);

  /**
   * This *must* be called when any exception was caught that prevented you from calling
   * {@link #onHeadersAvailable(int, Map)} and {@link #onBodyAvailable(InputStream)}. This will ensure that
   * {@link HalResourceLoader#getHalResource(String)} fails with an {@link HalApiClientException} with all information
   * that was already collected prior to the exception (e.g. status code).
   * @param ex the exception that was caught by your code
   */
  void onExceptionCaught(Throwable ex);
}
