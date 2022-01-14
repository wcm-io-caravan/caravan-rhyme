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

import static io.wcm.caravan.rhyme.api.common.RequestMetricsCollector.EMBED_RHYME_METADATA;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.client.HalApiClient;

/**
 * The controller that implements the {@link CompanyApi} interface, which defines the entry point of the API.
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

    // Since the controller class is directly implementing the CompanyApi interface we can simply return this.

    // Because CompanyApi extends LinkableResource, all methods from the interface will be automatically invoked later,
    // when the response is being rendered by the LinkableResourceMessageConverter.
    return this;
  }

  // To create the links in the entry point, we are simply delegating to the controllers that create
  // server-side implementations of each HAL API interface. When this entry point resource is being rendered,
  // the #createLink() method (but nothing else) of those related resources will be called, and
  // the link will be added to the response (using the relation from the @Related annotation of the
  // method declaration in the CompanyApi interface)

  @Override
  public EmployeeCollectionResource getEmployees() {
    return employees.findAll();
  }

  @Override
  public ManagerCollectionResource getManagers() {
    return managers.findAll();
  }

  // For any link *templates* defined in the CompanyApi interface, the response renderer will invoke
  // the corresponding method with a null value for the "id" parameter, since the ID is unknown at that point.
  // The resource implementations created by the controllers are able to handle this,
  // and will create a URI template with an "id" variable when #createLink() is being called during rendering.

  @Override
  public EmployeeResource getEmployeeById(Long id) {

    // Note that even though the ID is always null when this entry point is rendered as a HAL resource,
    // we still pass the given ID to the controller method.
    // This allows these methods to also be called directly by API consumers in the same application context
    // (which do know the ID of the entity they are looking for).
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
  public CompanyApi withClientPreferences(Boolean useEmbeddedResources, Boolean useFingerprinting, Boolean embedRhymeMetadata) {

    // create a link to this controller, but explicitly add template variables for the preference parameters,
    // which will be read by the CompanyApiLinkBuilder when a request using these parameters is processed
    return linkBuilder.create(linkTo(methodOn(CompanyApiController.class).get()))
        .withTemplateVariables(USE_EMBEDDED_RESOURCES, USE_FINGERPRINTING, EMBED_RHYME_METADATA)
        .withTitle("Reload the entry point with different settings")
        // since this method is never called by internal consumers, we don't have to return a full server-side
        // CompanyApi implementation. Instead we create and return a minimal proxy that only can render the link.
        .buildLinked(CompanyApi.class);
  }

  @Override
  public Link createLink() {

    // the link title should vary depending on whether the entry point was re-loaded via CompanyApi#withClientPreferences
    String title = "The entry point of the hypermedia example API";
    if (linkBuilder.hasClientPreferences()) {
      title += " (with custom client preferences)";
    }

    return linkBuilder.create(linkTo(methodOn(CompanyApiController.class).get()))
        .withTitle(title)
        .build();
  }
}
