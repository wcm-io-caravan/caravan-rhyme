package org.springframework.hateoas.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.spring.testing.MockMvcHalResourceLoader;

@SpringBootTest
public class SpringRhymeHypermediaIntegrationTest {

	private static final long NON_EXISTANT_ID = 999l;

	@Autowired
	private MockMvcHalResourceLoader resourceLoader;

	@Autowired
	private EmployeeRepository employees;

	@Autowired
	private ManagerRepository managers;

	private RootResource getEntryPoint() {

		HalApiClient apiClient = HalApiClient.create(resourceLoader);

		return apiClient.getRemoteResource("/", RootResource.class);
	}

	private Long getIdOfFirstEmployee() {

		return Iterables.firstOf(employees.findAll()).getId();
	}

	private Long getIdOfFirstManager() {

		return Iterables.firstOf(managers.findAll()).getId();
	}

	@Test
	public void getEmployees_should_list_employees_in_order_of_creation() throws Exception {

		List<EmployeeResource> employees = getEntryPoint().listAllEmployees().getEmployees();

		assertThat(employees).extracting(employee -> employee.getState().getName())//
				.containsExactly("Frodo", "Bilbo", "Sam", "Pippin");
	}

	@Test
	public void getManagers_should_list_managers_in_order_of_creation() throws Exception {

		List<ManagerResource> managers = getEntryPoint().listAllManagers().getManagers();

		assertThat(managers).extracting(manager -> manager.getState().getName())//
				.containsExactly("Gandalf", "Saruman");
	}

	@Test
	public void getEmployeeById_should_find_existing_employee() throws Exception {

		Long firstId = getIdOfFirstEmployee();

		Employee firstEmployee = getEntryPoint().getEmployeeById(firstId).getState();

		assertThat(firstEmployee.getId()).isEqualTo(firstId);
		assertThat(firstEmployee.getName()).isEqualTo("Frodo");
		assertThat(firstEmployee.getRole()).isEqualTo("ring bearer");
	}

	@Test
	public void getEmployeeById_should_respond_with_404_for_non_existing_id() throws Exception {

		HalApiClientException ex = catchThrowableOfType(
				() -> getEntryPoint().getEmployeeById(NON_EXISTANT_ID).getState(), HalApiClientException.class);

		assertThat(ex).isNotNull();
		assertThat(ex.getStatusCode()).isEqualTo(404);
	}

	@Test
	public void getManager_should_find_manager_for_employee() throws Exception {

		Long firstId = getIdOfFirstEmployee();

		EmployeeResource firstEmployee = getEntryPoint().getEmployeeById(firstId);
		Manager managerOfFirstEmployee = firstEmployee.getManager().getState();

		assertThat(managerOfFirstEmployee.getName()).isEqualTo("Gandalf");
	}

	@Test
	public void getManagerById_should_find_existing_manager() throws Exception {

		Long firstId = getIdOfFirstManager();

		Manager firstManager = getEntryPoint().getManagerById(firstId).getState();

		assertThat(firstManager.getId()).isEqualTo(firstId);
		assertThat(firstManager.getName()).isEqualTo("Gandalf");
	}

	@Test
	public void getManagerById_should_respond_with_404_for_non_existing_id() throws Exception {

		HalApiClientException ex = catchThrowableOfType(
				() -> getEntryPoint().getManagerById(NON_EXISTANT_ID).getState(), HalApiClientException.class);

		assertThat(ex).isNotNull();
		assertThat(ex.getStatusCode()).isEqualTo(404);
	}

	@Test
	public void getManagedEmployees_should_find_employees_of_manager() throws Exception {

		Long firstId = getIdOfFirstManager();

		ManagerResource firstManager = getEntryPoint().getManagerById(firstId);
		List<EmployeeResource> employeesOfFirstManager = firstManager.getManagedEmployees();

		assertThat(employeesOfFirstManager).extracting(employee -> employee.getState().getName())
				.containsExactly("Frodo", "Bilbo");
	}

	@Test
	public void getDetailedEmployeeById_should_find_existing_employee() throws Exception {

		Long firstId = getIdOfFirstEmployee();

		Employee firstEmployee = getEntryPoint().getDetailedEmployeeById(firstId).getState();

		assertThat(firstEmployee.getId()).isEqualTo(firstId);
		assertThat(firstEmployee.getName()).isEqualTo("Frodo");
	}

	@Test
	public void getDetailedEmployeeById_should_respond_with_404_for_non_existing_id() throws Exception {

		HalApiClientException ex = catchThrowableOfType(
				() -> getEntryPoint().getDetailedEmployeeById(NON_EXISTANT_ID).getState(), HalApiClientException.class);

		assertThat(ex).isNotNull();
		assertThat(ex.getStatusCode()).isEqualTo(404);
	}

	@Test
	public void getDetailedEmployeeById_should_list_colleages() throws Exception {

		Long firstId = getIdOfFirstEmployee();

		Stream<EmployeeResource> colleagues = getEntryPoint().getDetailedEmployeeById(firstId).getColleagues();

		assertThat(colleagues).extracting(colleague -> colleague.getState().getName())//
				.containsExactly("Bilbo");
	}

	@Test
	public void getDetailedEmployeeById_should_provide_manager() throws Exception {

		Long firstId = getIdOfFirstEmployee();

		Manager manager = getEntryPoint().getDetailedEmployeeById(firstId).getManager().getState();

		assertThat(manager.getName()).isEqualTo("Gandalf");
	}
}
