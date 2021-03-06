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
 * Thrown by the framework whenever an error condition is likely to be caused by invalid code (rather than network or
 * data issues)
 */
@ProviderType
public class HalApiDeveloperException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * @param message the detail message
   */
  public HalApiDeveloperException(String message) {
    super(message);
  }

  /**
   * @param message the detail message
   * @param cause the exception that was caught and re-thrown
   */
  public HalApiDeveloperException(String message, Throwable cause) {
    super(message, cause);
  }
}
