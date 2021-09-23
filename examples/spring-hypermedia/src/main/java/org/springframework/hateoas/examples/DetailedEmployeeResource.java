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

import static org.springframework.hateoas.examples.CompanyRelations.COLLEAGUE;
import static org.springframework.hateoas.examples.CompanyRelations.MANAGER;

import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * An extension of {@link EmployeeResource} that also provides details of the employee's manager,
 * and its direct colleagues as embedded resources. The main point of this resource is to show
 * how the {@link DetailedEmployeeController} is implementing this interface by combining data loaded
 * via HTTP from several other resources.
 */
@HalApiInterface
public interface DetailedEmployeeResource extends LinkableResource {

  /**
   * @return an {@link Employee} entity object suitable for JSON (de)serialization
   */
  @ResourceState
  Employee getState();

  /**
   * @return an embedded resource for the manager of this employee
   */
  @Related(MANAGER)
  ManagerResource getManager();

  /**
   * @return embedded resources for every other employee with the same manager
   */
  @Related(COLLEAGUE)
  Stream<EmployeeResource> getColleagues();
}
