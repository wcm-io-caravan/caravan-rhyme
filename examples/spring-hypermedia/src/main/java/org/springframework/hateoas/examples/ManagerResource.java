package org.springframework.hateoas.examples;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.CANONICAL;
import static org.springframework.hateoas.examples.CompanyRelations.EMPLOYEE;

import java.util.List;
import java.util.Optional;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface ManagerResource extends LinkableResource {

	@ResourceState
	Manager getState();

	@Related(EMPLOYEE)
	List<EmployeeResource> getManagedEmployees();

	@Related(CANONICAL)
	Optional<ManagerResource> getCanonical();
}
