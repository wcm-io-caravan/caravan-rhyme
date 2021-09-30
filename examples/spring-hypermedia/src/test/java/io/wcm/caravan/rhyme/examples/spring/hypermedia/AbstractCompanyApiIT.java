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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Iterables;
import org.springframework.beans.factory.annotation.Autowired;

import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;

/**
 * Defines a set of integration tests (running against the fully initialized Spring Boot application)
 * which cover most of the API's functionality.
 * The same set of tests are run three times (by the {@link ExternalClientIT}, {@link MockMvcClientIT} and
 * {@link ServerSideIT} subclasses), which is possible because the tests use the {@link CompanyApi} HAL API
 * interface to navigate to the resources under test.
 */
abstract class AbstractCompanyApiIT {

  private static final long NON_EXISTANT_ID = 999L;

  @Autowired
  private EmployeeRepository employeeRepository;
  @Autowired
  private ManagerRepository managerRepository;

  private CompanyApi api;

  @BeforeEach
  void setUp() {
    api = getApiImplementionOrClientProxy();
  }

  /**
   * This will be overridden in the subclasses to return either the server-side implementation of
   * the {@link CompanyApi} interface, or a dynamic client proxy that fetches the entry point
   * with a HTTP request.
   * @return the implementation of {@link CompanyApi} used to run the tests
   */
  protected abstract CompanyApi getApiImplementionOrClientProxy();

  // The repositories are initialized with the same DatabaseLoader that
  // is used when the application is started. For the tests that need to
  // know an existing ID, we just pick the first one that exists in each repo.

  protected Long getIdOfFirstEmployee() {

    return Iterables.firstOf(employeeRepository.findAll()).getId();
  }

  protected Long getIdOfFirstManager() {

    return Iterables.firstOf(managerRepository.findAll()).getId();
  }

  @Test
  public void getEmployees_should_list_employees_in_order_of_creation() throws Exception {

    List<EmployeeResource> employees = api.getEmployees().getAll();

    assertThat(employees)
        .extracting(employee -> employee.getState().getName())
        .containsExactly("Frodo", "Bilbo", "Sam", "Pippin");
  }

  @Test
  public void getManagers_should_list_managers_in_order_of_creation() throws Exception {

    List<ManagerResource> managers = api.getManagers().getAll();

    assertThat(managers)
        .extracting(manager -> manager.getState().getName())
        .containsExactly("Gandalf", "Saruman");
  }

  @Test
  public void getEmployeeById_should_find_existing_employee() throws Exception {

    Long firstId = getIdOfFirstEmployee();

    Employee firstEmployee = api.getEmployeeById(firstId).getState();

    assertThat(firstEmployee.getId()).isEqualTo(firstId);
    assertThat(firstEmployee.getName()).isEqualTo("Frodo");
    assertThat(firstEmployee.getRole()).isEqualTo("ring bearer");
  }

  @Test
  public void getEmployeeById_should_respond_with_404_for_non_existing_id() throws Exception {

    assertThat404isReturnedFor(
        () -> api.getEmployeeById(NON_EXISTANT_ID).getState());
  }

  @Test
  public void getManager_should_find_manager_for_employee() throws Exception {

    Long firstId = getIdOfFirstEmployee();

    EmployeeResource firstEmployee = api.getEmployeeById(firstId);
    Manager managerOfFirstEmployee = firstEmployee.getManager().getState();

    assertThat(managerOfFirstEmployee.getName()).isEqualTo("Gandalf");
  }

  @Test
  public void getCanonical_returns_same_instance_with_different_url() throws Exception {

    Long firstId = getIdOfFirstEmployee();

    ManagerResource manager = api.getEmployeeById(firstId).getManager();
    Optional<ManagerResource> canonical = manager.getCanonical();

    assertThat(canonical).isPresent();

    assertThat(canonical.get().getState().getId())
        .isEqualTo(manager.getState().getId());

    assertThat(canonical.get().createLink().getHref())
        .isNotEqualTo(manager.createLink().getHref());
  }

  @Test
  public void getManagerById_should_find_existing_manager() throws Exception {

    Long firstId = getIdOfFirstManager();

    Manager firstManager = api.getManagerById(firstId).getState();

    assertThat(firstManager.getId()).isEqualTo(firstId);
    assertThat(firstManager.getName()).isEqualTo("Gandalf");
  }

  @Test
  public void getManagerById_should_respond_with_404_for_non_existing_id() throws Exception {

    assertThat404isReturnedFor(
        () -> api.getManagerById(NON_EXISTANT_ID).getState());
  }

  @Test
  public void getCanonical_is_empty_if_manager_was_loaded_by_id() throws Exception {

    Long firstId = getIdOfFirstManager();

    Optional<ManagerResource> canonical = api.getManagerById(firstId).getCanonical();

    assertThat(canonical).isEmpty();
  }

  @Test
  public void getManagedEmployees_should_find_employees_of_manager() throws Exception {

    Long firstId = getIdOfFirstManager();

    ManagerResource firstManager = api.getManagerById(firstId);
    List<EmployeeResource> employeesOfFirstManager = firstManager.getManagedEmployees();

    assertThat(employeesOfFirstManager)
        .extracting(employee -> employee.getState().getName())
        .containsExactly("Frodo", "Bilbo");
  }

  @Test
  public void getDetailedEmployeeById_should_find_existing_employee() throws Exception {

    Long firstId = getIdOfFirstEmployee();

    Employee firstEmployee = api.getDetailedEmployeeById(firstId).getState();

    assertThat(firstEmployee.getId()).isEqualTo(firstId);
    assertThat(firstEmployee.getName()).isEqualTo("Frodo");
  }

  @Test
  public void getDetailedEmployeeById_should_respond_with_404_for_non_existing_id() throws Exception {

    assertThat404isReturnedFor(
        () -> api.getDetailedEmployeeById(NON_EXISTANT_ID).getState());
  }

  @Test
  public void getDetailedEmployeeById_should_list_colleages() throws Exception {

    Long firstId = getIdOfFirstEmployee();

    Stream<EmployeeResource> colleagues = api.getDetailedEmployeeById(firstId).getColleagues();

    assertThat(colleagues)
        .extracting(colleague -> colleague.getState().getName())
        .containsExactly("Bilbo");
  }

  @Test
  public void getDetailedEmployeeById_should_provide_manager() throws Exception {

    Long firstId = getIdOfFirstEmployee();

    Manager manager = api.getDetailedEmployeeById(firstId).getManager().getState();

    assertThat(manager.getName()).isEqualTo("Gandalf");
  }

  private void assertThat404isReturnedFor(ThrowingCallable codeThatThrows) {

    Throwable ex = catchThrowable(codeThatThrows);

    assertThat(ex).isNotNull()
        .withFailMessage("No exception was thrown by the given callable");

    if (ex instanceof HalApiClientException) {
      assertThat(((HalApiClientException)ex).getStatusCode())
          .isEqualTo(404);
    }
    else if (ex instanceof HalApiServerException) {
      assertThat(((HalApiServerException)ex).getStatusCode())
          .isEqualTo(404);
    }
    else {
      fail("An unexpected exception was thrown", ex);
    }
  }
}
