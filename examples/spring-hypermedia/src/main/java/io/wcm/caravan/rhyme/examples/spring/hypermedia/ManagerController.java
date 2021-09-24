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
 * The controller that creates server-side {@link ManagerResource} and {@link ManagerCollectionResource} instances
 */
@RestController
class ManagerController {

  @Autowired
  private ManagerRepository repository;

  // inject the controllers for all related resources
  @Autowired
  private CompanyApi api;
  @Autowired
  private EmployeeController employees;

  @Autowired
  private TimestampedLinkBuilder linkBuilder;

  /**
   * A controller method to create a {@link ManagerCollectionResource} that lists all managers in the database. This
   * method is called to render that resource for an incoming HTTP request, but also to render any link to this kind of
   * resource.
   * @return a server-side implementation of {@link ManagerCollectionResource}
   */
  @GetMapping("/managers")
  ManagerCollectionResource findAll() {

    return new ManagerCollectionResource() {

      @Override
      public List<ManagerResource> getAll() {

        return StreamUtils.createResourcesFrom(repository.findAll(), ManagerResourceImpl::new);
      }

      @Override
      public CompanyApi getApi() {
        return api;
      }

      @Override
      public EmployeeCollectionResource getEmployees() {
        return employees.findAll();
      }

      @Override
      public Link createLink() {

        // every link to the controller for this type of resource is created here, with the help of Spring's MvcLinkBuilder
        return linkBuilder.create(linkTo(methodOn(ManagerController.class).findAll()))
            .withTitle("A collection of all managers")
            .build();
      }
    };
  }

  /**
   * A controller method to create a {@link ManagerResource} for a specific employee. This is called
   * to render this resource for an incoming HTTP request, but also to render all links to this kind of resource.
   * @param id of the employee, or null if this method is called to create the link template in the {@link CompanyApi}
   * @return a server-side implementation of {@link ManagerResource}
   */
  @GetMapping("/managers/{id}")
  ManagerResource findById(@PathVariable Long id) {

    // Create and return server-side implementation of the resource
    return new ManagerResourceImpl(id, () -> repository.findById(id)
        // If no entity is found with the given ID, throwing an exception from which a status code can be extracted
        // will make Rhyme#renderResponse return a vnd.error response with that status code
        .orElseThrow(() -> new HalApiServerException(404, "No entity was found with id " + id)));
  }

  /**
   * The server-side implementation of the {@link ManagerResource} interface, which also allows for this resource
   * to be embedded in the context from which it was created.
   */
  private class ManagerResourceImpl implements ManagerResource, EmbeddableResource {

    private final Long id;
    private final Lazy<Manager> state;

    private final boolean embedded;

    /**
     * A constructor that defers loading of the {@link Manager} to when {@link #getState()} is called by the renderer.
     * This is important because instance creation should be as fast as possible, as it also happens when only
     * a link to an employee with a given ID is rendered (for which we don't need the full Manager entity).
     * @param id of the manager
     * @param entityLoader a function that loads the manager from the {@link ManagerRepository}
     */
    protected ManagerResourceImpl(Long id, Supplier<Manager> entityLoader) {
      this.id = id;
      this.state = Lazy.of(entityLoader);
      this.embedded = false;
    }

    /**
     * The constructor to create an *embedded* resource for an employee that was already loaded from the database
     * @param manager an entity loaded from the {@link ManagerRepository}
     */
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
      return employees.findEmployeesOfManagerWithId(id);
    }

    @Override
    public Optional<ManagerResource> getCanonical() {
      // the canonical link is only present for variations of this resource
      // (which can be done by subclassing as you can in #findManagerOfEmployeeWithId)
      return Optional.empty();
    }

    @Override
    public boolean isEmbedded() {
      return embedded;
    }

    @Override
    public Link createLink() {

      // every link to the controller for this type of resource is created here, with the help of Spring's MvcLinkBuilder
      return linkBuilder.create(linkTo(methodOn(ManagerController.class).findById(id)))
          .withTitle("The manager with ID " + id)
          .withTemplateTitle("A link template to load a single manager by ID")
          .build();
    }
  }

  /**
   * A controller method to create a {@link ManagerResource} for the manager of a given employee (with a path below the
   * path of the employee)
   * @param id of the employee,
   * @return a server-side implementation of {@link ManagerResource}
   */
  @GetMapping("/employees/{id}/manager")
  ManagerResource findManagerOfEmployeeWithId(@PathVariable Long id) {

    // find the manager in the repository
    Manager manager = repository.findByEmployeesId(id);

    // Create a resource implementation (but don't use the constructor used to create embedded resources)
    return new ManagerResourceImpl(manager.getId(), () -> manager) {

      @Override
      public Optional<ManagerResource> getCanonical() {
        // include a link to the version of this resource that uses a path with the manager's id
        return Optional.of(findById(manager.getId()));
      }

      @Override
      public Link createLink() {
        // overridden so that the path from this controller method is used
        return linkBuilder.create(linkTo(methodOn(ManagerController.class).findManagerOfEmployeeWithId(id)))
            // since the manager instance was already loaded, we can also give a bit more context in the link title for this resource
            .withTitle("The manager (" + manager.getName() + ")  of the employee with id " + id)
            .build();
      }
    };
  }
}
