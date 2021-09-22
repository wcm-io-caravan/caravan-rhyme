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

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.CANONICAL;
import static org.springframework.hateoas.examples.CompanyRelations.EMPLOYEE;

import java.util.List;
import java.util.Optional;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface ManagerResource extends LinkableResource {

  @ResourceState
  Manager getState();

  @Related(EMPLOYEE)
  List<EmployeeResource> getManagedEmployees();

  @Related(CANONICAL)
  Optional<ManagerResource> getCanonical();
}
