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
import java.util.Optional;
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
class ManagerController {

  @Autowired
  private ManagerRepository repository;

  @Autowired
  private RootController rootController;

  @Autowired
  private EmployeeController employees;

  /**
   * Look up all managers, and transform them into a REST collection resource
   */
  @GetMapping("/managers")
  ManagersResource findAll() {

    return new ManagersResource() {

      @Override
      public List<ManagerResource> getManagers() {

        return StreamUtils.mapEntitiesToListOfResources(repository.findAll(), ManagerResourceImpl::new);
      }

      @Override
      public RootResource getRoot() {

        return rootController.root();
      }

      @Override
      public EmployeesResource getEmployees() {

        return employees.findAll();
      }

      @Override
      public Link createLink() {

        return new Link(linkTo(methodOn(ManagerController.class).findAll()).toString())
            .setTitle("A collection of all managers");
      }
    };
  }

  /**
   * Look up a single {@link Manager} and transform it into a REST resource using
   * @param id
   */
  @GetMapping("/managers/{id}")
  ManagerResource findOne(@PathVariable Long id) {

    return new ManagerResourceImpl(id, () -> repository.findById(id)
        .orElseThrow(() -> new HalApiServerException(404, "No entity was found with id " + id)));
  }

  private class ManagerResourceImpl implements ManagerResource, EmbeddableResource {

    private final Long id;
    private final Lazy<Manager> state;

    private final boolean embedded;

    protected ManagerResourceImpl(Long id, Supplier<Manager> stateSupplier) {
      this.id = id;
      this.state = Lazy.of(stateSupplier);
      this.embedded = false;
    }

    private ManagerResourceImpl(Manager manager) {
      this.id = manager.getId();
      this.state = Lazy.of(manager);
      this.embedded = true;
    }

    @Override
    public Manager getState() {

      return state.get();
    }

    @Override
    public List<EmployeeResource> getManagedEmployees() {

      return employees.findEmployeesOfManager(id);
    }

    @Override
    public Optional<ManagerResource> getCanonical() {

      return Optional.empty();
    }

    @Override
    public boolean isEmbedded() {

      return embedded;
    }

    @Override
    public Link createLink() {

      return new Link(linkTo(methodOn(ManagerController.class).findOne(id)).toString()).setTitle(
          id == null ? "A link template to load a single manager by ID" : "The manager with ID " + id);
    }
  }

  /**
   * Find an {@link Employee}'s {@link Manager} based upon employee id. Turn it
   * into a context-based link.
   * @param id
   * @return
   */
  @GetMapping("/employees/{id}/manager")
  ManagerResource findManager(@PathVariable Long id) {

    Long employeeId = id;

    Manager manager = repository.findByEmployeesId(id);

    return new ManagerResourceImpl(manager.getId(), () -> manager) {

      @Override
      public Optional<ManagerResource> getCanonical() {

        return Optional.of(findOne(id));
      }

      @Override
      public Link createLink() {

        return new Link(linkTo(methodOn(ManagerController.class).findManager(employeeId)).toString())
            .setTitle("The manager (" + manager.getName() + ")  of the employee with id " + employeeId);
      }
    };
  }
}
