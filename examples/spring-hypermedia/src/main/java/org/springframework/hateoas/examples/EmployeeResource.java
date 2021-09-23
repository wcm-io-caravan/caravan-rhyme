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

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * A HAL resource that represents a single employee
 */
@HalApiInterface
public interface EmployeeResource extends LinkableResource {

  /**
   * @return an {@link Employee} entity object suitable for JSON (de)serialization
   */
  @ResourceState
  Employee getState();

  /**
   * @return a link to the manager of this employee
   */
  @Related("company:manager")
  ManagerResource getManager();

  /**
   * @return a link to a more detailed resource for this employee, which embeds full details on the employee's manager
   *         and colleagues
   */
  @Related("company:detailedEmployee")
  DetailedEmployeeResource getDetails();
}
