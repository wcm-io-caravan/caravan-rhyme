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
package io.wcm.caravan.rhyme.osgi.sampleservice.api.errors;

import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariables;

/**
 * an interface to be used as {@link TemplateVariables} parameter for
 * some {@link Related} methods in the {@link ErrorExamplesResource}
 */
public interface ErrorParameters {

  /**
   * @return the HTTP status code to be sent in the response header
   */
  Integer getStatusCode();

  /**
   * @return the message of the exception that will be thrown by the resource implementation
   */
  String getMessage();

  /**
   * @return true if the exception should not be thrown directly, but as cause of another wrapping exception
   */
  Boolean getWrapException();
}
