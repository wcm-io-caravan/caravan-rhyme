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

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.hal.resource.Link;

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

	/**
	 * Look up all employees, and transform them into a REST collection resource
	 * 
	 */
	@GetMapping("/employees")
	EmployeesResource findAll() {

		return new EmployeesResource() {

			@Override
			public List<EmployeeResource> getEmployees() {

				return StreamUtils.transform(repository.findAll(), EmployeeResourceImpl::new);
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
	 *
	 * @param id
	 */
	@GetMapping("/employees/{id}")
	EmployeeResource findOne(@PathVariable Long id) {

		return new EmployeeResourceImpl(id, () -> repository.findById(id));
	}

	List<EmployeeResource> findEmployeesOfManager(long managerId) {

		return StreamUtils.transform(repository.findByManagerId(managerId), EmployeeResourceImpl::new);
	}

	private final class EmployeeResourceImpl extends AbstractEntityResource<Employee> implements EmployeeResource {

		private EmployeeResourceImpl(Long id, Supplier<Optional<Employee>> supplier) {
			super(id, supplier);
		}

		private EmployeeResourceImpl(Employee employee) {
			super(employee.getId(), employee);
		}

		@Override
		public ManagerResource getManager() {

			return managers.findManager(id);
		}

		@Override
		public Link createLink() {

			return new Link(linkTo(methodOn(EmployeeController.class).findOne(id)).toString()).setTitle(
					id == null ? "A link template to load a single employee by ID" : "The employee with ID " + id);
		}
	}
}
