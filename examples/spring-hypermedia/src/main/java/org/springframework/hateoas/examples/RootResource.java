package org.springframework.hateoas.examples;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface RootResource extends LinkableResource {

	@Related("hal:employees")
	EmployeesResource getEmployees();

	@Related("hal:managers")
	ManagersResource getManagers();

	@Related("hal:employee")
	EmployeeResource getEmployeeById(@TemplateVariable("id") Long id);

	@Related("hal:manager")
	ManagerResource getManagerById(@TemplateVariable("id") Long id);
}
