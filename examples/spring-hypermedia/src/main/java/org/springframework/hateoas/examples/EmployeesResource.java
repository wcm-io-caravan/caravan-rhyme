package org.springframework.hateoas.examples;

import java.util.List;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface EmployeesResource extends LinkableResource {

	@Related("employees")
	List<EmployeeResource> getEmployees();

	@Related("root")
	RootResource getRoot();

	@Related("managers")
	ManagersResource getManagers();
}
