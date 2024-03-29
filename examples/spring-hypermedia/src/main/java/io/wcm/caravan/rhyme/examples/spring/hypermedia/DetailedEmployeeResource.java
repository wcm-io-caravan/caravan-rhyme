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
package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import java.util.stream.Stream;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * An alternative to {@link EmployeeResource} that is aggregating properties from difference resources, and also
 * provides details of the employee's manager, and its direct colleagues as embedded resources. The main point of this
 * resource is to show how the {@link DetailedEmployeeController} is implementing this interface by combining data
 * loaded via HTTP from several other resources.
 */
@HalApiInterface
public interface DetailedEmployeeResource extends LinkableResource {

  /**
   * @return an {@link Employee} entity object suitable for JSON (de)serialization
   */
  @ResourceState
  Employee getState();

  /**
   * @return the name of the manager
   */
  @ResourceProperty("manager")
  String getManagerName();

  /**
   * @return the manager of this employee
   */
  @Related("company:manager")
  ManagerResource getManager();

  /**
   * @return every other employee with the same manager
   */
  @Related("company:colleague")
  Stream<EmployeeResource> getColleagues();

  /**
   * @return a link to an external HTML profile page for the employee
   */
  @Related("company:htmlProfile")
  Link getHtmlProfileLink();
}
