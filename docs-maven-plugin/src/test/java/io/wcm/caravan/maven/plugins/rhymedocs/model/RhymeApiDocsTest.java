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
package io.wcm.caravan.maven.plugins.rhymedocs.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestEntryPoint;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestResource;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.TestRelations;
import io.wcm.caravan.maven.plugins.rhymedocs.model.RhymeResourceDocs.RhymeRelatedMethodDocs;
import io.wcm.caravan.maven.plugins.rhymedocs.model.RhymeResourceDocs.RhymeVariableDocs;


public class RhymeApiDocsTest {

  public static final String[] TEST_INTERFACES = {
      RhymeDocTestEntryPoint.class.getName(),
      RhymeDocTestResource.class.getName()
  };

  private static RhymeApiDocs apiDocs;

  @BeforeAll
  static void setUp() {
    apiDocs = new RhymeApiDocs(Paths.get("src/test/java"), RhymeApiDocsTest.class.getClassLoader());
  }

  private static RhymeResourceDocs getDocsFor(Class<?> halApiInterface) {

    return apiDocs.getResourceDocs().stream()
        .filter(docs -> docs.getFullyQualifiedClassName().equals(halApiInterface.getName()))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("No documentation found dor " + halApiInterface));
  }

  @Test
  public void getResourceDocs_should_return_docs_for_all_HalApiInterfaces() throws Exception {

    List<RhymeResourceDocs> resourceDocs = apiDocs.getResourceDocs();

    assertThat(resourceDocs).hasSize(TEST_INTERFACES.length);

    assertThat(resourceDocs).extracting(RhymeResourceDocs::getFullyQualifiedClassName)
        .containsExactlyInAnyOrder(TEST_INTERFACES);
  }

  @Test
  public void RhymeResourceDocs_getTitle_should_return_short_name() {

    RhymeResourceDocs entryPointDocs = getDocsFor(RhymeDocTestEntryPoint.class);

    assertThat(entryPointDocs.getTitle())
        .isEqualTo(RhymeDocTestEntryPoint.class.getSimpleName());
  }

  @Test
  public void RhymeResourceDocs_getDescription_should_return_javadoc_comment() {

    RhymeResourceDocs entryPointDocs = getDocsFor(RhymeDocTestEntryPoint.class);

    assertThat(entryPointDocs.getDescription())
        .isEqualTo("Javadoc for entrypoint interface");
  }

  private RhymeRelatedMethodDocs getEntryPointDocsForRelation(String relation) {

    RhymeResourceDocs entryPointDocs = getDocsFor(RhymeDocTestEntryPoint.class);

    return findDocsForRelation(relation, entryPointDocs);
  }

  private RhymeRelatedMethodDocs getTestResourceDocsForRelation(String relation) {

    RhymeResourceDocs resourceDocs = getDocsFor(RhymeDocTestResource.class);

    return findDocsForRelation(relation, resourceDocs);
  }

  private RhymeRelatedMethodDocs findDocsForRelation(String relation, RhymeResourceDocs resourceDocs) {

    return resourceDocs.getRelations().stream()
        .filter(docs -> relation.equals(docs.getRelation()))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("No documentation found for relation " + relation));
  }

  @Test
  public void RhymeResourceDocs_getRelations_should_contain_javadoc_comment() {

    RhymeRelatedMethodDocs multipleDocs = getEntryPointDocsForRelation(TestRelations.MULTIPLE);

    assertThat(multipleDocs.getDescription())
        .isEqualTo("Javadoc for getMultiple");
  }

  @Test
  public void RhymeResourceDocs_getRelations_should_contain_related_resource_title() {

    RhymeRelatedMethodDocs multipleDocs = getEntryPointDocsForRelation(TestRelations.MULTIPLE);

    assertThat(multipleDocs.getRelatedResourceTitle())
        .isEqualTo(RhymeDocTestResource.class.getSimpleName());
  }

  @Test
  public void RhymeResourceDocs_getRelations_should_have_null_related_resource_title_for_LinkableResource() {

    RhymeRelatedMethodDocs externalDocs = getEntryPointDocsForRelation(TestRelations.EXTERNAL);

    assertThat(externalDocs.getRelatedResourceTitle())
        .isNull();
  }

  @Test
  public void RhymeResourceDocs_getRelations_should_contain_related_resource_href() {

    RhymeRelatedMethodDocs multipleDocs = getEntryPointDocsForRelation(TestRelations.MULTIPLE);

    assertThat(multipleDocs.getRelatedResourceHref())
        .isEqualTo(RhymeDocTestResource.class.getName() + ".html");
  }

  @Test
  public void RhymeResourceDocs_getRelations_should_have_null_related_resource_href_for_LinkableResource() {

    RhymeRelatedMethodDocs externalDocs = getEntryPointDocsForRelation(TestRelations.EXTERNAL);

    assertThat(externalDocs.getRelatedResourceHref())
        .isNull();
  }

  @Test
  public void RhymeResourceDocs_getRelations_should_contain_cardinality_for_multiple() {

    RhymeRelatedMethodDocs multipleDocs = getEntryPointDocsForRelation(TestRelations.MULTIPLE);

    assertThat(multipleDocs.getCardinality())
        .isEqualTo("0..n");
  }

  @Test
  public void RhymeResourceDocs_getRelations_should_contain_cardinality_for_optional() {

    RhymeRelatedMethodDocs optionalDocs = getEntryPointDocsForRelation(TestRelations.OPTIONAL);

    assertThat(optionalDocs.getCardinality())
        .isEqualTo("0..1");
  }

  @Test
  public void RhymeResourceDocs_getRelations_should_contain_cardinality_for_single() {

    RhymeRelatedMethodDocs singleDocs = getEntryPointDocsForRelation(TestRelations.SINGLE);

    assertThat(singleDocs.getCardinality())
        .isEqualTo("1");
  }

  @Test
  public void RhymeResourceDocs_getRelations_should_handle_methods_with_template() {

    RhymeRelatedMethodDocs singleDocs = getEntryPointDocsForRelation(TestRelations.TEMPLATE);

    assertThat(singleDocs.getRelation())
        .isEqualTo(TestRelations.TEMPLATE);
  }

  @Test
  public void RhymeRelationDocs_getVariables_should_handle_simple_template() {

    RhymeRelatedMethodDocs templateDocs = getEntryPointDocsForRelation(TestRelations.TEMPLATE);

    assertThat(templateDocs.getVariables()).hasSize(1);

    RhymeVariableDocs variable = templateDocs.getVariables().get(0);

    assertThat(variable.getName()).isEqualTo("foo");
    assertThat(variable.getType()).isEqualTo("String");
    assertThat(variable.getDescription()).isEqualTo("Javadoc for foo parameter");
  }

  @Test
  public void RhymeRelationDocs_getVariables_should_keep_order_of_multiple_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.MULTI_VAR_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getName)
        .containsExactly("integer", "collection", "booleanWithoutJavadocs");
  }

  @Test
  public void RhymeRelationDocs_getVariables_should_handle_missing_javadocs_of_multiple_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.MULTI_VAR_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getDescription)
        .containsExactly("Javadoc comment for integer", "Javadoc comment for collection", "");
  }

  @Test
  public void RhymeRelationDocs_getVariables_should_extract_types_of_multiple_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.MULTI_VAR_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getType)
        .containsExactly("Integer", "List", "Boolean");
  }

  @Test
  public void RhymeRelationDocs_getVariables_should_use_alphabetical_order_of_interface_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.INTERFACE_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getName)
        .containsExactly("booleanWithoutJavadocs", "collection", "integer");
  }

  @Test
  public void RhymeRelationDocs_getVariables_should_handle_missing_javadocs_of_interface_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.INTERFACE_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getDescription)
        .containsExactly("", "Javadoc comment for collection", "Javadoc comment for integer");
  }

  @Test
  public void RhymeRelationDocs_getVariables_should_extract_types_of_interface_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.INTERFACE_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getType)
        .containsExactly("Boolean", "List", "Integer");
  }

  @Test
  public void RhymeRelationDocs_getVariables_should_use_original_order_of_class_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.CLASS_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getName)
        .containsExactly("integer", "collection", "booleanWithoutJavadocs");
  }

  @Test
  public void RhymeRelationDocs_getVariables_should_handle_missing_javadocs_of_class_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.CLASS_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getDescription)
        .containsExactly("Javadoc comment for integer", "Javadoc comment for collection", "");
  }

  @Test
  public void RhymeRelationDocs_getVariables_should_extract_types_of_class_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.CLASS_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getType)
        .containsExactly("Integer", "List", "Boolean");
  }
}
