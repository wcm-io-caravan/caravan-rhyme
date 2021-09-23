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

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.CANONICAL;

import java.util.List;
import java.util.Optional;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * A HAL resource that represents a single manager
 */
@HalApiInterface
public interface ManagerResource extends LinkableResource {

  /**
   * @return an {@link Manager} entity object suitable for JSON (de)serialization
   */
  @ResourceState
  Manager getState();

  /**
   * @return the employees that are assigned to this manager
   */
  @Related("company:employee")
  List<EmployeeResource> getManagedEmployees();

  /**
   * @return an optional link to the preferred URL for this resource, that is only present when this resource was loaded
   *         through the company:manager link of an {@link EmployeeResource}
   */
  @Related(CANONICAL)
  Optional<ManagerResource> getCanonical();
}
