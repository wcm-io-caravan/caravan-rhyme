/*
 * Copyright 2017 the original author or authors.
 *
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
 */
package org.springframework.hateoas.examples;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.server.ResourceConversions;
import io.wcm.caravan.rhyme.spring.api.SpringRhyme;

/**
 * @author Greg Turnquist
 */
@RestController
class DetailedEmployeeController {

	@Autowired
	private SpringRhyme rhyme;

	@GetMapping("/employees/{id}/detailed")
	DetailedEmployeeResource findOne(@PathVariable Long id) {

		return new DetailedEmployeeResource() {

			private final RootResource root = rhyme.getRemoteResource("http://localhost:8081", RootResource.class);

			private EmployeeResource getEmployee() {

				return root.getEmployeeById(id);
			}

			@Override
			public Employee getState() {

				return getEmployee().getState();
			}

			@Override
			public ManagerResource getManager() {

				ManagerResource manager = getEmployee().getManager();

				return ResourceConversions.asEmbeddedResourceWithoutLink(manager);
			}

			@Override
			public Stream<EmployeeResource> getColleagues() {

				return getEmployee().getManager().getManagedEmployees().stream()
						.filter(employee -> employee.getState().getId() != id)
						.map(ResourceConversions::asEmbeddedResourceWithoutLink);
			}

			@Override
			public Link createLink() {

				return new Link(linkTo(methodOn(DetailedEmployeeController.class).findOne(id)).toString())
						.setTitle(id == null ? "A link template to detailed data for single employee by ID"
								: "The employee with ID " + id
										+ ", with embedded resources for her managers and colleagues");
			}
		};
	}
}
