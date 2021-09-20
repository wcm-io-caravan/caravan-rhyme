package org.springframework.hateoas.examples;

import static org.springframework.hateoas.examples.CompanyRelations.EMPLOYEES;
import static org.springframework.hateoas.examples.CompanyRelations.MANAGER;
import static org.springframework.hateoas.examples.CompanyRelations.ROOT;

import java.util.List;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface ManagersResource extends LinkableResource {

	@Related(MANAGER)
	List<ManagerResource> getManagers();
	
	@Related(ROOT)
	RootResource getRoot();
	
	@Related(EMPLOYEES)
	EmployeesResource getEmployees();
}
