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
package io.wcm.caravan.rhyme.api.common;

import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.hal.resource.HalResource;

/**
 * A response to a successful or failed HTTP request to retrieve a {@link HalResource}
 */
public class HalResponse {

  private final Integer status;
  private final String contentType;
  private final String reason;
  private final HalResource body;
  private final Integer maxAge;

  /**
   * Creates an instance with all fields set to null, you have to use the #withXyz method to actually populate the
   * fields
   */
  public HalResponse() {
    this.status = null;
    this.contentType = null;
    this.reason = null;
    this.body = null;
    this.maxAge = null;
  }

  private HalResponse(Integer status, String contentType, String reason, HalResource body, Integer maxAge) {
    this.status = status;
    this.contentType = contentType;
    this.reason = reason;
    this.body = body;
    this.maxAge = maxAge;
  }

  /**
   * @return the HTTP status code (or null if a request failed without receiving a status code)
   */
  public Integer getStatus() {
    return status;
  }

  /**
   * @param value (or null if a request failed without receiving a status code)
   * @return a new instance with the given status code
   */
  public HalResponse withStatus(Integer value) {
    return new HalResponse(value, contentType, reason, body, maxAge);
  }

  /**
   * @return the value of the 'Content-Type' HTTP header
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * @param value the value of the 'Content-Type' HTTP header
   * @return a new instance with the given content type
   */
  public HalResponse withContentType(String value) {
    return new HalResponse(status, value, reason, body, maxAge);
  }

  /**
   * @param value the reason from the HTTP status line
   * @return a new instance with the given reason
   */
  public HalResponse withReason(String value) {
    return new HalResponse(status, contentType, value, body, maxAge);
  }

  /**
   * @return the JSON response body as a {@link HalResource} instance (or null if a client-side request failed without
   *         receiving a response body)
   */
  public HalResource getBody() {
    return body;
  }

  /**
   * @param value the JSON response body as a {@link HalResource} instance
   * @return a new instance with the given body
   */
  public HalResponse withBody(HalResource value) {
    return new HalResponse(status, contentType, reason, value, maxAge);
  }

  /**
   * @param value the JSON response body as a Jackson {@link JsonNode} instance
   * @return a new instance with the given body
   */
  public HalResponse withBody(JsonNode value) {
    HalResource hal = value != null ? new HalResource(value) : null;
    return new HalResponse(status, contentType, reason, hal, maxAge);
  }

  /**
   * @return the "max-age" value of the "Cache-Control" header (or null if not defined)
   */
  public Integer getMaxAge() {
    return maxAge;
  }

  /**
   * @param value the "max-age" value of the "Cache-Control" header (or null if not defined)
   * @return a new instance with the given max age
   */
  public HalResponse withMaxAge(Integer value) {
    return new HalResponse(status, contentType, reason, body, value);
  }
}
