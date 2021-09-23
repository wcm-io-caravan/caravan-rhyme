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
 * The controller that creates server-side {@link EmployeeResource} and {@link EmployeeCollectionResource} instances
 */
@RestController
class EmployeeController {

  @Autowired
  private EmployeeRepository repository;

  // inject the controllers for all related resources
  @Autowired
  private RootController rootController;
  @Autowired
  private ManagerController managers;
  @Autowired
  private DetailedEmployeeController detailedEmployees;

  /**
   * A controller method to create a {@link EmployeeCollectionResource} that lists all employees in the database. This
   * method is called to render that resource for an incoming HTTP request, but also to render any link to this kind of
   * resource.
   * @return a server-side implementation of {@link EmployeeCollectionResource}
   */
  @GetMapping("/employees")
  EmployeeCollectionResource findAll() {

    // Create and return an implementation of the HAL API interface which defines the resource structure.
    // All methods will be automatically invoked later, when the response is being rendered
    // by the LinkableResourceMessageConverter.
    return new EmployeeCollectionResource() {

      @Override
      public List<EmployeeResource> getAll() {

        // create an embedded resource implementation for every employee in the repository
        return StreamUtils.createResourcesFrom(repository.findAll(), EmployeeResourceImpl::new);
      }

      // The links to the other resources are created by simply returning a server-side
      // resource implementation created by the corresponding controllers. The renderer will
      // the call #createLink() on these resources to create the links

      @Override
      public RootResource getRoot() {

        return rootController.createEntryPoint();
      }

      @Override
      public ManagerCollectionResource getManagers() {

        return managers.findAll();
      }

      @Override
      public Link createLink() {

        // All logic for URL construction is handled by Sprint HATEOAS' WebMvcLinkBuilder.
        return new Link(linkTo(methodOn(EmployeeController.class).findAll()).toString())
            .setTitle("A collection of all employees");
      }
    };
  }

  /**
   * A controller method to create a {@link EmployeeResource} for a specific employee. This is called
   * to render this resource for an incoming HTTP request, but also to render all links to this kind of resource.
   * @param id of the employee, or null if this method is called to create the link template in the {@link RootResource}
   * @return a server-side implementation of {@link EmployeeResource}
   */
  @GetMapping("/employees/{id}")
  EmployeeResource findById(@PathVariable Long id) {

    // Create and return server-side implementation of the resource
    return new EmployeeResourceImpl(id, () -> repository.findById(id)
        // If no entity is found with the given ID, throwing an exception from which a status code can be extracted
        // will make the VndErrorHandlingControllerAdvice return a vnd.error response with that status code
        .orElseThrow(() -> new HalApiServerException(404, "No entity was found with id " + id)));
  }

  /**
   * @param id the id of a manager
   * @return a list of embeddable {@link EmployeeResource} instances
   */
  List<EmployeeResource> findEmployeesOfManagerWithId(long id) {

    // create an embedded resource implementation for every employee managed by the given amanger
    return StreamUtils.createResourcesFrom(repository.findByManagerId(id), EmployeeResourceImpl::new);
  }

  /**
   * The server-side implementation of the {@link EmployeeResource} interface, which also allows for this resource
   * to be embedded in the context from which it was created.
   */
  private final class EmployeeResourceImpl implements EmployeeResource, EmbeddableResource {

    private final Long id;
    private final Lazy<Employee> state;

    private final boolean embedded;

    /**
     * A constructor that defers loading of the {@link Employee} to when {@link #getState()} is called by the renderer.
     * This is important because instance creation should be as fast as possible, as it also happens when only
     * a link to an employee with a given ID is rendered (for which we don't need the full Employee entity).
     * @param id of the employee
     * @param entityLoader a function that loads the employee from the {@link EmployeeRepository}
     */
    private EmployeeResourceImpl(Long id, Supplier<Employee> entityLoader) {
      this.id = id;
      this.state = Lazy.of(entityLoader);
      this.embedded = false;
    }

    /**
     * The constructor to create an *embedded* resource for an employee that was already loaded from the database
     * @param employee an entity loaded from the {@link EmployeeRepository}
     */
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
      return managers.findManagerOfEmployeeWithId(id);
    }

    @Override
    public DetailedEmployeeResource getDetails() {
      return detailedEmployees.findById(id);
    }

    @Override
    public boolean isEmbedded() {
      return embedded;
    }

    @Override
    public Link createLink() {

      // All logic for URL construction is handled by Sprint HATEOAS' WebMvcLinkBuilder.
      return new Link(linkTo(methodOn(EmployeeController.class).findById(id)).toString()).setTitle(
          // In addition, we specify different titles to be used for link templates and resolved links (including the self-link)
          id == null ? "A link template to load a single employee by ID" : "The employee with ID " + id);
    }
  }
}
