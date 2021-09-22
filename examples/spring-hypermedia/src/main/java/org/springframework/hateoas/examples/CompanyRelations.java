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

public final class CompanyRelations {

  private CompanyRelations() {
    // this class only contains static constants
  }

  public static final String COLLEAGUE = "company:colleague";

  public static final String DETAILED_EMPLOYEE = "company:detailedEmployee";

  public static final String EMPLOYEE = "company:employee";

  public static final String EMPLOYEES = "company:employees";

  public static final String MANAGER = "company:manager";

  public static final String MANAGERS = "company:managers";

  public static final String ROOT = "company:root";
}
