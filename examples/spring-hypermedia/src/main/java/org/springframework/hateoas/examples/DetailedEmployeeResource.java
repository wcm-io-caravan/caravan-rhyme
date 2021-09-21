package org.springframework.hateoas.examples;

import static org.springframework.hateoas.examples.CompanyRelations.COLLEAGUE;
import static org.springframework.hateoas.examples.CompanyRelations.MANAGER;

import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface DetailedEmployeeResource extends LinkableResource {

	@ResourceState
	Employee getState();

	@Related(MANAGER)
	ManagerResource getManager();

	@Related(COLLEAGUE)
	Stream<EmployeeResource> getColleagues();
}
