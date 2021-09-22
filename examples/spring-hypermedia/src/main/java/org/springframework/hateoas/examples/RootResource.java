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

import static org.springframework.hateoas.examples.CompanyRelations.DETAILED_EMPLOYEE;
import static org.springframework.hateoas.examples.CompanyRelations.EMPLOYEE;
import static org.springframework.hateoas.examples.CompanyRelations.EMPLOYEES;
import static org.springframework.hateoas.examples.CompanyRelations.MANAGER;
import static org.springframework.hateoas.examples.CompanyRelations.MANAGERS;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface RootResource extends LinkableResource {

  @Related(EMPLOYEES)
  EmployeesResource listAllEmployees();

  @Related(MANAGERS)
  ManagersResource listAllManagers();

  @Related(EMPLOYEE)
  EmployeeResource getEmployeeById(@TemplateVariable("id") Long id);

  @Related(MANAGER)
  ManagerResource getManagerById(@TemplateVariable("id") Long id);

  @Related(DETAILED_EMPLOYEE)
  DetailedEmployeeResource getDetailedEmployeeById(@TemplateVariable("id") Long id);
}
