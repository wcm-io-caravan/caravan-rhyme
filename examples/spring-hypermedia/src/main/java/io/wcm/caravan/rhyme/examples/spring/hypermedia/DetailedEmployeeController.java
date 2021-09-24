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

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.server.ResourceConversions;
import io.wcm.caravan.rhyme.spring.api.SpringRhyme;

/**
 * An example for a controller that does not create the resources directly from the database,
 * but is instead loading other {@link EmployeeResource} and {@link ManagerResource} by HTTP.
 * It combines them into a {@link DetailedEmployeeResource} with embedded resources, so all of this information
 * can be fetched with a single request.
 * While this is a bit over-complicated here (where the repositories are also available locally) all this would work
 * the same way if you wanted to aggregate or enrich resources from an external API.
 * @see HalApiClient more details on how the client proxies work
 */
@RestController
class DetailedEmployeeController {

  /**
   * Inject the same request-scoped {@link SpringRhyme} component that is also used to render the resource.
   * This allows to include detailed information about all HTTP requests executed by this controller in the
   * embedded "rhyme:metadata" resource. It also ensures that the max-age cache directives of this controller's
   * response are properly calculated (i.e. the max-age of upstream resources and cached content is taken
   * into account)
   */
  @Autowired
  private SpringRhyme rhyme;

  /**
   * A controller method to create a {@link DetailedEmployeeResource} for a specific employee. This is called
   * to render this resource for an incoming HTTP request, but also to render all links to this kind of resource.
   * @param id of the employee, or null if this method is called to create the link template in the {@link CompanyApi}
   * @return a server-side implementation of {@link DetailedEmployeeResource}
   * @throws HalApiClientException if any of the HTTP requests to load required resources failed
   */
  @GetMapping("/employees/{id}/detailed") //  Note that this is the only location (including tests) where the path of this resource is specified.
  DetailedEmployeeResource findById(@PathVariable Long id) {

    // Create and return an implementation of the HAL API interface which defines the resource structure.
    // All methods will be automatically invoked later, when the response is being rendered
    // by the LinkableResourceMessageConverter.
    return new DetailedEmployeeResource() {

      // Create a dynamic client proxy that can load the API's entry point (and all related resources) by HTTP.
      // Even though all resource implementations should be constructed as fast as possible, it doesn't hurt to create the proxy right here.
      // This is because the actual HTTP requests will only be executed when a method on a proxy is called.
      private final CompanyApi entryPoint = rhyme.getRemoteResource("http://localhost:8081", CompanyApi.class);

      /**
       * Load the entry point and expand the "company:employee" link template with the given ID,
       * @return another proxy object that knows how to fetch the EmployeeResource from the expanded URL.
       */
      private EmployeeResource getEmployee() {

        return entryPoint.getEmployeeById(id);
      }

      @Override
      public Employee getState() {

        // This fetches the upstream employee resource, and deserializes the state from its JSON properties.
        // Since this resource should have exactly the same state, we can simply return the instance without modifications.
        return getEmployee().getState();
      }

      @Override
      public ManagerResource getManager() {

        // Get a resource client proxy that follows the link to the manager of the employee.
        // Even though getEmployee() is called multiple times in this class, it's not required to store that proxy instance anywhere,
        // since repeated method calls with the same parameter will quickly return the same proxy instance.
        ManagerResource manager = getEmployee().getManager();

        // We could return this resource proxy directly, but then only a link to the upstream resource would be added.
        // Since we want to embed the manager resource, we need to convert it to another proxy that also implements EmbeddableResource.
        return ResourceConversions.asEmbeddedResourceWithoutLink(manager);
      }

      @Override
      public Stream<EmployeeResource> getColleagues() {

        // create a stream from the links to all employees managed by the same manager
        return getEmployee().getManager().getManagedEmployees().stream()
            // ignore the employee for which we are just generating the detailed resource
            .filter(employee -> !employee.getState().getId().equals(id))
            // and again ensure that these resources are embedded rather than linked
            .map(ResourceConversions::asEmbeddedResourceWithoutLink);
      }

      @Override
      public Link createLink() {

        // All logic for URL construction is handled by Sprint HATEOAS' WebMvcLinkBuilder.
        return new Link(linkTo(methodOn(DetailedEmployeeController.class).findById(id)).toString())
            // In addition, we specify different titles to be used for link templates and resolved links (including the self-link)
            .setTitle(id == null ? "A link template to detailed data for single employee by ID"
                : "The employee with ID " + id + ", with embedded resources for her managers and colleagues");
      }
    };
  }
}