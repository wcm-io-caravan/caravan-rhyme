package org.springframework.hateoas.examples;

import java.util.List;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface EmployeesResource extends LinkableResource {

	@Related(CompanyRelations.EMPLOYEE)
	List<EmployeeResource> getEmployees();

	@Related(CompanyRelations.ROOT)
	RootResource getRoot();

	@Related(CompanyRelations.MANAGERS)
	ManagersResource getManagers();
}
