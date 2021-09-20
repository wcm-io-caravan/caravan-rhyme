package org.springframework.hateoas.examples;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface RootResource extends LinkableResource {

	@Related("employees")
	EmployeesResource getEmployees();
	
	@Related("managers")
	ManagersResource getManagers();
}
