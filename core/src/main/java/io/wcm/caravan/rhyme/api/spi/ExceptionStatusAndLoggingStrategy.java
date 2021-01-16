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
package io.wcm.caravan.rhyme.api.spi;

import org.osgi.annotation.versioning.ConsumerType;

import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;

/**
 * Allows users of this framework to specify how {@link VndErrorResponseRenderer} handles specific exceptions that the
 * framework doesn't know about
 */
@ConsumerType
public interface ExceptionStatusAndLoggingStrategy {

  /**
   * @param error an exception that was thrown when rendering a resource
   * @return the status code that should be used for the response
   */
  default Integer extractStatusCode(Throwable error) {
    return null;
  }

  /**
   * @param error an exception that was thrown when rendering a resource
   * @return true if this exception should not be logged as an error with full stack trace
   */
  default boolean logAsCompactWarning(Throwable error) {
    return false;
  }

  /**
   * @param error an exception that was thrown when rendering a resource
   * @return the message to be displayed in the VND+error resource (and in logging)
   */
  default String getErrorMessageWithoutRedundantInformation(Throwable error) {
    return error.getMessage();
  }
}
