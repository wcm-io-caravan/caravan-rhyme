/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.spring.testing.MockMvcHalResourceLoader;

@SpringBootTest
public class SpringRhymeHypermediaIntegrationTest {

  private static final long NON_EXISTANT_ID = 999L;

  @Autowired
  private EmployeeRepository employeeRepository;
  @Autowired
  private ManagerRepository managerRepository;

  @Autowired
  private MockMvcHalResourceLoader mockMvcResourceLoader;

  private RootResource getEntryPoint() {

    // Create a HalApiClient that is using spring's MockMvc to simulate actual HTTP requests
    // coming into the currently running WebApplicationContext.
    HalApiClient apiClient = HalApiClient.create(mockMvcResourceLoader);

    // All of these tests in this class will start with fetching the single entry point
    // (exactly as an external consumer would), and then follow links to other resources as required.
    return apiClient.getRemoteResource("/", RootResource.class);
  }

  private Long getIdOfFirstEmployee() {

    return Iterables.firstOf(employeeRepository.findAll()).getId();
  }

  private Long getIdOfFirstManager() {

    return Iterables.firstOf(managerRepository.findAll()).getId();
  }

  private void assertThatClientFailsWith404(ThrowingCallable codeThatThrows) {

    HalApiClientException ex = catchThrowableOfType(codeThatThrows, HalApiClientException.class);

    assertThat(ex).isNotNull();
    assertThat(ex.getStatusCode()).isEqualTo(404);
  }

  @Test
  public void getEmployees_should_list_employees_in_order_of_creation() throws Exception {

    List<EmployeeResource> employees = getEntryPoint().getEmployees().getAll();

    assertThat(employees)
        .extracting(employee -> employee.getState().getName())
        .containsExactly("Frodo", "Bilbo", "Sam", "Pippin");
  }

  @Test
  public void getManagers_should_list_managers_in_order_of_creation() throws Exception {

    List<ManagerResource> managers = getEntryPoint().getManagers().getAll();

    assertThat(managers)
        .extracting(manager -> manager.getState().getName())
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

    assertThatClientFailsWith404(
        () -> getEntryPoint().getEmployeeById(NON_EXISTANT_ID).getState());
  }

  @Test
  public void getManager_should_find_manager_for_employee() throws Exception {

    Long firstId = getIdOfFirstEmployee();

    EmployeeResource firstEmployee = getEntryPoint().getEmployeeById(firstId);
    Manager managerOfFirstEmployee = firstEmployee.getManager().getState();

    assertThat(managerOfFirstEmployee.getName()).isEqualTo("Gandalf");
  }

  @Test
  public void getCanonical_returns_same_instance_with_different_url() throws Exception {

    Long firstId = getIdOfFirstEmployee();

    ManagerResource manager = getEntryPoint().getEmployeeById(firstId).getManager();
    Optional<ManagerResource> canonical = manager.getCanonical();

    assertThat(canonical).isPresent();

    assertThat(canonical.get().getState())
        .isEqualTo(manager.getState());

    assertThat(canonical.get().createLink().getHref())
        .isNotEqualTo(manager.createLink().getHref());
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

    assertThatClientFailsWith404(
        () -> getEntryPoint().getManagerById(NON_EXISTANT_ID).getState());
  }

  @Test
  public void getCanonical_is_empty_if_manager_was_loaded_by_id() throws Exception {

    Long firstId = getIdOfFirstManager();

    Optional<ManagerResource> canonical = getEntryPoint().getManagerById(firstId).getCanonical();

    assertThat(canonical).isEmpty();
  }

  @Test
  public void getManagedEmployees_should_find_employees_of_manager() throws Exception {

    Long firstId = getIdOfFirstManager();

    ManagerResource firstManager = getEntryPoint().getManagerById(firstId);
    List<EmployeeResource> employeesOfFirstManager = firstManager.getManagedEmployees();

    assertThat(employeesOfFirstManager)
        .extracting(employee -> employee.getState().getName())
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

    assertThatClientFailsWith404(
        () -> getEntryPoint().getDetailedEmployeeById(NON_EXISTANT_ID).getState());
  }

  @Test
  public void getDetailedEmployeeById_should_list_colleages() throws Exception {

    Long firstId = getIdOfFirstEmployee();

    Stream<EmployeeResource> colleagues = getEntryPoint().getDetailedEmployeeById(firstId).getColleagues();

    assertThat(colleagues)
        .extracting(colleague -> colleague.getState().getName())
        .containsExactly("Bilbo");
  }

  @Test
  public void getDetailedEmployeeById_should_provide_manager() throws Exception {

    Long firstId = getIdOfFirstEmployee();

    Manager manager = getEntryPoint().getDetailedEmployeeById(firstId).getManager().getState();

    assertThat(manager.getName()).isEqualTo("Gandalf");
  }
}
