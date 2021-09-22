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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;

/**
 * @author Greg Turnquist
 */
@RestController
class EmployeeController {

  @Autowired
  private EmployeeRepository repository;

  @Autowired
  private RootController rootController;

  @Autowired
  private ManagerController managers;

  @Autowired
  private DetailedEmployeeController details;

  /**
   * Look up all employees, and transform them into a REST collection resource
   */
  @GetMapping("/employees")
  EmployeesResource findAll() {

    return new EmployeesResource() {

      @Override
      public List<EmployeeResource> getEmployees() {

        return StreamUtils.mapEntitiesToListOfResources(repository.findAll(), EmployeeResourceImpl::new);
      }

      @Override
      public RootResource getRoot() {

        return rootController.root();
      }

      @Override
      public ManagersResource getManagers() {

        return managers.findAll();
      }

      @Override
      public Link createLink() {

        return new Link(linkTo(methodOn(EmployeeController.class).findAll()).toString())
            .setTitle("A collection of all employees");
      }
    };
  }

  /**
   * Look up a single {@link Employee} and transform it into a REST resource
   * @param id
   */
  @GetMapping("/employees/{id}")
  EmployeeResource findOne(@PathVariable Long id) {

    return new EmployeeResourceImpl(id, () -> repository.findById(id)
        .orElseThrow(() -> new HalApiServerException(404, "No entity was found with id " + id)));
  }

  List<EmployeeResource> findEmployeesOfManager(long managerId) {

    return StreamUtils.mapEntitiesToListOfResources(repository.findByManagerId(managerId),
        EmployeeResourceImpl::new);
  }

  private final class EmployeeResourceImpl implements EmployeeResource, EmbeddableResource {

    private final Long id;
    private final Lazy<Employee> state;

    private final boolean embedded;

    private EmployeeResourceImpl(Long id, Supplier<Employee> stateSupplier) {
      this.id = id;
      this.state = Lazy.of(stateSupplier);
      this.embedded = false;
    }

    private EmployeeResourceImpl(Employee employee) {
      this.id = employee.getId();
      this.state = Lazy.of(employee);
      this.embedded = true;
    }

    @Override
    public Employee getState() {

      return state.get();
    }

    @Override
    public ManagerResource getManager() {

      return managers.findManager(id);
    }

    @Override
    public DetailedEmployeeResource getDetails() {

      return details.findOne(id);
    }

    @Override
    public boolean isEmbedded() {

      return embedded;
    }

    @Override
    public Link createLink() {

      return new Link(linkTo(methodOn(EmployeeController.class).findOne(id)).toString()).setTitle(
          id == null ? "A link template to load a single employee by ID" : "The employee with ID " + id);
    }
  }


}
