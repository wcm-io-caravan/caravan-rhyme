/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.rhyme.api.exceptions;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.HalResponse;

/**
 * Thrown whenever the {@link HalApiClient} implementation fails to load a HAL resource, and provides
 * access to the status code and URL of the failed HTTP request.
 */
public class HalApiClientException extends RuntimeException {

  private static final long serialVersionUID = -2265405683402689209L;

  private final HalResponse errorResponse;
  private final String requestUrl;

  /**
   * Constructor to use if the request failed with a non-successful HTTP status code
   * @param errorResponse the error response as received from the upstream service
   * @param requestUrl the URI of the request that failed
   * @param cause the root cause for the failed HTTP request
   */
  public HalApiClientException(HalResponse errorResponse, String requestUrl, Throwable cause) {
    super("HTTP request failed with status code " + errorResponse.getStatus(), cause);
    this.errorResponse = errorResponse;
    this.requestUrl = requestUrl;
  }

  /**
   * Constructor to use if no response body was received from the upstream service.
   * @param message additional information on where and why the exception happened
   * @param statusCode the status code received from the upstream server (or null if the request failed without
   *          receiving a status code)
   * @param requestUrl the URI of the request that failed
   * @param cause the root cause for the failed HTTP request
   */
  public HalApiClientException(String message, Integer statusCode, String requestUrl, Throwable cause) {
    super(message, cause);
    this.errorResponse = new HalResponse().withStatus(statusCode);
    this.requestUrl = requestUrl;
  }

  /**
   * Constructor used to re-throw a {@link HalApiClientException} and add additional context information
   * @param message additional information on where and why the exception happened
   * @param cause the exception that was caught and re-thrown
   */
  public HalApiClientException(String message, HalApiClientException cause) {
    super(message, cause);
    this.errorResponse = cause.getErrorResponse();
    this.requestUrl = cause.getRequestUrl();
  }

  /**
   * @return a {@link HalResponse} object that contains additional information in the body only if the upstream
   *         service's response contained a Vnd+Error body
   */
  public HalResponse getErrorResponse() {
    return errorResponse;
  }

  /**
   * @return the HTTP status code from the upstream request (or null if the request failed without receiving a status
   *         code)
   */
  public Integer getStatusCode() {
    return errorResponse.getStatus();
  }

  /**
   * @return the URI of the request that failed
   */
  public String getRequestUrl() {
    return requestUrl;
  }

}
