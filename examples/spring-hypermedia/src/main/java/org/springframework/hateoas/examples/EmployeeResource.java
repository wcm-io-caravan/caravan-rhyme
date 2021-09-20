package org.springframework.hateoas.examples;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface EmployeeResource extends LinkableResource {

	@ResourceState
	Employee getState();

	@Related("manager")
	ManagerResource getManager();
}
