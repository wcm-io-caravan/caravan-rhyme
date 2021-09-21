package org.springframework.hateoas.examples;

import static org.springframework.hateoas.examples.CompanyRelations.EMPLOYEE;
import static org.springframework.hateoas.examples.CompanyRelations.MANAGERS;
import static org.springframework.hateoas.examples.CompanyRelations.ROOT;

import java.util.List;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface EmployeesResource extends LinkableResource {

	@Related(EMPLOYEE)
	List<EmployeeResource> getEmployees();

	@Related(ROOT)
	RootResource getRoot();

	@Related(MANAGERS)
	ManagersResource getManagers();
}
