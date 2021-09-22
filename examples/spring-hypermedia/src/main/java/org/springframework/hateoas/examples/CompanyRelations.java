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
package org.springframework.hateoas.examples;

import io.wcm.caravan.rhyme.api.annotations.Related;

/**
 * Defines constants used in the {@link Related} annotations in the HAL API interfaces.
 * All relations are using a curie prefix ("company:") so that a documentation link template will be added
 * to the rendered resources and shown by tools such as the HAL Browser or HAL Explorer.
 */
final class CompanyRelations {

  private CompanyRelations() {
    // this class only contains static constants
  }

  static final String COLLEAGUE = "company:colleague";

  static final String DETAILED_EMPLOYEE = "company:detailedEmployee";

  static final String EMPLOYEE = "company:employee";

  static final String EMPLOYEES = "company:employees";

  static final String MANAGER = "company:manager";

  static final String MANAGERS = "company:managers";

  static final String ROOT = "company:root";
}
