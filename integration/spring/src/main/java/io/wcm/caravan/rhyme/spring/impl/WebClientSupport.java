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
package io.wcm.caravan.rhyme.spring.impl;

import java.io.ByteArrayInputStream;
import java.net.URI;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;
import io.wcm.caravan.rhyme.spring.api.WebClientProvider;
import reactor.core.publisher.Mono;

public final class WebClientSupport implements HttpClientSupport {

  private final WebClientProvider webClientProvider;

  public WebClientSupport(WebClientProvider webClientProvider) {this.webClientProvider = webClientProvider;}

  @Override
  public void executeGetRequest(URI uri, HttpClientCallback callback) {
    webClientProvider.webClientForUri(uri)
        .get().uri(uri).retrieve()
        // any 200 responses will be parsed as a string and forwarded to the callback
        .toEntity(byte[].class).doOnSuccess(entity -> handleOkResponse(callback, entity))
        // any responses with error status should be handled specifically, as we want to
        // pass the status code and headers to the callback, and try to parse the body
        // as it may contain vnd.error information
        .onErrorResume(WebClientResponseException.class, ex -> handleErrorResponse(callback, ex))
        // any other exceptions thrown during the request or while handling the response
        // should be caught as well
        .doOnError(callback::onExceptionCaught)
        // finally, subscribe so that the request is actually executed
        .subscribe();
  }

  private void handleOkResponse(HttpClientCallback callback, ResponseEntity<byte[]> entity) {

    callback.onHeadersAvailable(entity.getStatusCode().value(), entity.getHeaders());

    byte[] body = ObjectUtils.defaultIfNull(entity.getBody(), new byte[0]);

    callback.onBodyAvailable(new ByteArrayInputStream(body));
  }

  private Mono<ResponseEntity<byte[]>> handleErrorResponse(HttpClientCallback callback,
      WebClientResponseException ex) {

    callback.onHeadersAvailable(ex.getStatusCode().value(), ex.getHeaders());

    callback.onBodyAvailable(new ByteArrayInputStream(ex.getResponseBodyAsByteArray()));

    // since we have successfully called onBodyAvailable, all information from the
    // exception has been collected by the callback, and there is no need for
    // further error exception handling
    return Mono.empty();
  }
}
