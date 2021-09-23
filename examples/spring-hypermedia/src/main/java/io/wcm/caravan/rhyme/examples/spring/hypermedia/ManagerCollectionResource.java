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

import java.util.List;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * A HAL resource with embedded data for multiple managers
 */
@HalApiInterface
public interface ManagerCollectionResource extends LinkableResource {

  /**
   * @return all individual managers
   */
  @Related("company:manager")
  List<ManagerResource> getAll();

  /**
   * @return a link back to the entry point of the API
   */
  @Related("company:root")
  RootResource getRoot();

  /**
   * @return a link to a similar collection of all employees
   */
  @Related("company:employees")
  EmployeeCollectionResource getEmployees();
}
