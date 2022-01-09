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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.client.HalApiClient;

/**
 * The controller that implements the {@link CompanyApi} interface, which define the entry point of the API.
 * <p>
 * If there is a consumer of this API running in the same application, it can simply add an {@link Autowired}
 * field of type {@link CompanyApi} to work directly with the server-side implementations of the API's resources.
 * This consumer will then be using the same public interfaces as external clients that are using {@link HalApiClient}
 * or {@link Rhyme#getRemoteResource(String, Class)} to access this API.
 * This makes it easy to divide a larger system into decoupled modules from the beginning,
 * without adding the overhead and complexity of having multiple microservice applications, but you can easily
 * break a system apart at a later stage if required.
 * </p>
 */
@RestController
class CompanyApiController implements CompanyApi {

  // inject the controllers for all resources that are linked from the entry point
  @Autowired
  private EmployeeController employees;
  @Autowired
  private ManagerController managers;
  @Autowired
  private DetailedEmployeeController detailedEmployees;

  @Autowired
  private CompanyApiLinkBuilder linkBuilder;

  /**
   * A controller method used to render the entry point of the API as a HAL+JSON response.
   * @return a server-side implementation of {@link CompanyApi}
   */
  @GetMapping("/")
  CompanyApi get() {

    return this;
  }

  // All methods from the interface will be automatically invoked later, when the response is being rendered
  // by the LinkableResourceMessageConverter.

  @Override
  public EmployeeCollectionResource getEmployees() {
    return employees.findAll();
  }

  @Override
  public ManagerCollectionResource getManagers() {
    return managers.findAll();
  }

  @Override
  public EmployeeResource getEmployeeById(Long id) {

    // Note that even though the ID is always null when this entry point is rendered as a HAL resource,
    // we still pass the given ID to the controller method. This allows these methods
    // to also be called directly by API consumers in the same application (which do know the ID of
    // the entity they are looking for).
    return employees.findById(id);
  }

  @Override
  public ManagerResource getManagerById(Long id) {
    return managers.findById(id);
  }

  @Override
  public DetailedEmployeeResource getDetailedEmployeeById(Long id) {
    return detailedEmployees.findById(id);
  }

  @Override
  public CompanyApi withSettings(Boolean useEmbeddedResources, Boolean useFingerprinting) {

    return linkBuilder.create(linkTo(methodOn(CompanyApiController.class).get()))
        .withTemplateVariables(USE_EMBEDDED_RESOURCES, USE_FINGERPRINTING)
        .withTitle("Reload the entry point with different settings")
        .buildProxyOf(CompanyApi.class);
  }

  @Override
  public Link createLink() {

    return linkBuilder.create(linkTo(methodOn(CompanyApiController.class)
        .get()))
        .withTitle("The entry point of the hypermedia example API")
        .build();
  }
}
