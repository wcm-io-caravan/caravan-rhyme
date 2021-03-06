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
package io.wcm.caravan.rhyme.api.exceptions;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A server-side exception that you can throw in your resource implementations if you want to send a response with a
 * specific status code
 */
@ProviderType
public class HalApiServerException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int statusCode;

  /**
   * @param statusCode to use for the HTTP response
   * @param msg the error message
   */
  public HalApiServerException(int statusCode, String msg) {
    this(statusCode, msg, null);
  }

  /**
   * @param statusCode to use for the HTTP response
   * @param msg the error message
   * @param cause the root cause
   */
  public HalApiServerException(int statusCode, String msg, Throwable cause) {
    super(msg, cause);
    this.statusCode = statusCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
