package org.springframework.hateoas.examples;

public final class CompanyRelations {

  private CompanyRelations() {
    // this class only contains static constants
  }

  public static final String COLLEAGUE = "company:colleague";

  public static final String DETAILED_EMPLOYEE = "company:detailedEmployee";

  public static final String EMPLOYEE = "company:employee";

  public static final String EMPLOYEES = "company:employees";

  public static final String MANAGER = "company:manager";

  public static final String MANAGERS = "company:managers";

  public static final String ROOT = "company:root";
}
