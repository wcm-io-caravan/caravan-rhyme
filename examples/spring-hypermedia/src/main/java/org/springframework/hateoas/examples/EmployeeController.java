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
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Lazy;
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
	public EmployeesResource findAll() {

		return new EmployeeCollectionResourceImpl(() -> repository.findAll(), ctrl -> ctrl.findAll());
	}

	/**
	 * Look up a single {@link Employee} and transform it into a REST resource
	 *
	 * @param id
	 */
	@GetMapping("/employees/{id}")
	public EmployeeResource findOne(@PathVariable long id) {

		return new EmployeeResourceImpl(id, () -> repository.findById(id));
	}

	/**
	 * Find an {@link Employee}'s {@link Manager} based upon employee id. Turn it
	 * into a context-based link.
	 *
	 * @param id
	 * @return
	 */
	@GetMapping("/managers/{id}/employees")
	public EmployeesResource findEmployees(@PathVariable long id) {

		return new EmployeeCollectionResourceImpl(() -> repository.findByManagerId(id), ctrl -> ctrl.findEmployees(id));
	}

	private final class EmployeeResourceImpl extends AbstractEntityResource<Employee> implements EmployeeResource {

		private EmployeeResourceImpl(long id, Supplier<Optional<Employee>> supplier) {
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
			return new Link(linkTo(methodOn(EmployeeController.class).findOne(id)).toString());
		}
	}

	private final class EmployeeCollectionResourceImpl implements EmployeesResource {

		private final Lazy<Iterable<Employee>> employees;
		private final Function<EmployeeController, ?> controllerFunc;

		public EmployeeCollectionResourceImpl(Supplier<Iterable<Employee>> employees,
				Function<EmployeeController, ?> controllerFunc) {
			this.employees = Lazy.of(employees);
			this.controllerFunc = controllerFunc;
		}

		@Override
		public List<EmployeeResource> getEmployees() {

			return StreamUtils.transform(employees.get(), EmployeeResourceImpl::new);
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

			return new Link(linkTo(controllerFunc.apply(methodOn(EmployeeController.class))).toString());
		}

	}
}
