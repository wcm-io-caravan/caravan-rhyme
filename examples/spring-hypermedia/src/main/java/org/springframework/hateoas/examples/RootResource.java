package org.springframework.hateoas.examples;

import static org.springframework.hateoas.examples.CompanyRelations.EMPLOYEE;
import static org.springframework.hateoas.examples.CompanyRelations.EMPLOYEES;
import static org.springframework.hateoas.examples.CompanyRelations.MANAGER;
import static org.springframework.hateoas.examples.CompanyRelations.MANAGERS;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface RootResource extends LinkableResource {

	@Related(EMPLOYEES)
	EmployeesResource listAllEmployees();

	@Related(MANAGERS)
	ManagersResource listAllManagers();

	@Related(EMPLOYEE)
	EmployeeResource getEmployeeById(@TemplateVariable("id") Long id);

	@Related(MANAGER)
	ManagerResource getManagerById(@TemplateVariable("id") Long id);
}
