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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.client.HalApiClient;

/**
 * The controller to create {@link RootResource} instances defining the entry point of the API.
 */
@RestController
public class RootController {

  // inject the controllers for all resources that are linked from the entry point
  @Autowired
  private EmployeeController employees;
  @Autowired
  private ManagerController managers;
  @Autowired
  private DetailedEmployeeController details;

  /**
   * A controller method to create a {@link RootResource}. This is called to render this resource for an incoming HTTP
   * request, but also to render all links to this kind of resource.
   * <p>
   * If there is a consumer of this API running in the same application, it can also be called directly to gain access
   * to the API's functionality using the same interfaces as external clients that are using {@link HalApiClient}
   * or {@link Rhyme#getRemoteResource(String, Class)} to access this API through dynamic client proxies.
   * This makes it easy to break a system apart into multiple microservices at a later stage.
   * </p>
   * @return a server-side implementation of {@link RootResource}
   */
  @GetMapping("/")
  public RootResource createEntryPoint() {

    // Create and return an implementation of the HAL API interface which defines the resource structure.
    // All methods will be automatically invoked later, when the response is being rendered
    // by the LinkableResourceMessageConverter.
    return new RootResource() {

      // To create the links in the entry point, we are simply delegating to the controllers that create
      // server-side implementations of each HAL API interface. When this root resource is being rendered,
      // the #createLink() method (but nothing else) of those related resources will be called, and
      // the link will be added to the response (using the relation from the @Related annotation of the
      // method declaration in the RootResource interface)

      @Override
      public EmployeesResource getEmployees() {

        return employees.findAll();
      }

      @Override
      public ManagersResource getManagers() {

        return managers.findAll();
      }

      // For any link *templates* defined in the RootResource interface, the response renderer will invoke
      // the corresponding method with a null value for the "id" parameter, since the ID is unknown at that point.
      // The resource implementations created by the controllers are able to handle this,
      // and will create a URI template with an "id" variable when #createLink() is being called during rendering.

      @Override
      public EmployeeResource getEmployeeById(Long id) {

        // Note that even though the ID is always null when this entry point is rendered as a HAL resource,
        // we still pass the given ID to the controller method. This allows these methods
        // to also be called directly by API consumers in the same application (which do know the ID of
        // the entity they are looking for).

        return employees.findOne(id);
      }

      @Override
      public ManagerResource getManagerById(Long id) {

        return managers.findOne(id);
      }

      @Override
      public DetailedEmployeeResource getDetailedEmployeeById(Long id) {

        return details.findOne(id);
      }

      @Override
      public Link createLink() {

        // All logic for URL construction is handled by Sprint HATEOAS' WebMvcLinkBuilder.
        return new Link(linkTo(methodOn(RootController.class).createEntryPoint()).toString())
            // We only add a title to be shown for each link to the entry point (including the self link)
            .setTitle("The entry point of the hypermedia example API");
      }
    };
  }
}
