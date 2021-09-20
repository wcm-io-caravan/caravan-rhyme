package org.springframework.hateoas.examples;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface ManagerResource extends LinkableResource {

	@ResourceState
	Manager getState();

	@Related("employees")
	EmployeesResource getEmployees();
}
