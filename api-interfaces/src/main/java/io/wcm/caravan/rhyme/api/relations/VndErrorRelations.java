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
package io.wcm.caravan.rhyme.api.relations;

/**
 * Link relations used by the application/vnd.error+json media type
 */
public final class VndErrorRelations {

  private VndErrorRelations() {
    // no need to instantiate this class, as it contains only constants
  }

  /**
   * Relation to use for links to the resources for which an error has occured
   */
  public static final String ABOUT = "about";

  /**
   * Relation to use for embedded resources with add
   */
  public static final String ERRORS = "errors";

}
