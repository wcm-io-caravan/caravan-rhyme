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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.hal.resource.Link;

/**
 * @author Greg Turnquist
 */
@RestController
class RootController {

	@Autowired
	private EmployeeController employees;

	@Autowired
	private ManagerController managers;

	@Autowired
	private DetailedEmployeeController details;

	@GetMapping("/")
	RootResource root() {

		return new RootResource() {

			@Override
			public EmployeesResource listAllEmployees() {

				return employees.findAll();
			}

			@Override
			public ManagersResource listAllManagers() {

				return managers.findAll();
			}

			@Override
			public EmployeeResource getEmployeeById(Long id) {

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

				return new Link(linkTo(methodOn(RootController.class).root()).toString())
						.setTitle("The entry point of the hypermedia example API");
			}

		};
	}
}
