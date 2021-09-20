package org.springframework.hateoas.examples;

import java.util.List;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface ManagersResource extends LinkableResource {

	@Related("managers")
	List<ManagerResource> getManagers();
	
	@Related("root")
	RootResource getRoot();
	
	@Related("employees")
	EmployeesResource getEmployees();
}
