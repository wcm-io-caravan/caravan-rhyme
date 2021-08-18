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

import static io.wcm.caravan.maven.plugins.rhymedocs.model.RhymeApiDocsTest.getDocsFor;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestEntryPoint;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestResource;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.TestRelations;

public class RhymeRelationDocsTest {

  @Test
  public void RhymeResourceDocs_getRelations_should_contain_all_relations_used_in_interface() {

    RhymeResourceDocs entryPointDocs = getDocsFor(RhymeDocTestEntryPoint.class);

    assertThat(entryPointDocs.getRelations())
        .extracting(RhymeRelatedMethodDocs::getRelation)
        .contains(TestRelations.MULTIPLE, TestRelations.OPTIONAL, TestRelations.SINGLE, TestRelations.EXTERNAL, TestRelations.TEMPLATE);
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
  public void getDescription_should_return_javadoc_comment() {

    RhymeRelatedMethodDocs multipleDocs = getEntryPointDocsForRelation(TestRelations.MULTIPLE);

    assertThat(multipleDocs.getDescription())
        .isEqualTo("Javadoc for getMultiple");
  }

  @Test
  public void getRelatedResourceTitle_should_return_class_name() {

    RhymeRelatedMethodDocs multipleDocs = getEntryPointDocsForRelation(TestRelations.MULTIPLE);

    assertThat(multipleDocs.getRelatedResourceTitle())
        .isEqualTo(RhymeDocTestResource.class.getSimpleName());
  }

  @Test
  public void getRelatedResourceTitle_should_return_null_for_LinkableResource() {

    RhymeRelatedMethodDocs externalDocs = getEntryPointDocsForRelation(TestRelations.EXTERNAL);

    assertThat(externalDocs.getRelatedResourceTitle())
        .isNull();
  }

  @Test
  public void getRelatedResourceHref_should_return_html_file_name() {

    RhymeRelatedMethodDocs multipleDocs = getEntryPointDocsForRelation(TestRelations.MULTIPLE);

    assertThat(multipleDocs.getRelatedResourceHref())
        .isEqualTo(RhymeDocTestResource.class.getName() + ".html");
  }

  @Test
  public void getRelatedResourceHref_should_return_null_for_LinkableResource() {

    RhymeRelatedMethodDocs externalDocs = getEntryPointDocsForRelation(TestRelations.EXTERNAL);

    assertThat(externalDocs.getRelatedResourceHref())
        .isNull();
  }

  @Test
  public void getCardinality_should_be_zero_to_n_for_collections() {

    RhymeRelatedMethodDocs multipleDocs = getEntryPointDocsForRelation(TestRelations.MULTIPLE);

    assertThat(multipleDocs.getCardinality())
        .isEqualTo("0..n");
  }

  @Test
  public void getCardinality_should_be_zero_to_1_for_optionals() {

    RhymeRelatedMethodDocs optionalDocs = getEntryPointDocsForRelation(TestRelations.OPTIONAL);

    assertThat(optionalDocs.getCardinality())
        .isEqualTo("0..1");
  }

  @Test
  public void getCardinality_should_be_1_for_regular_object() {

    RhymeRelatedMethodDocs singleDocs = getEntryPointDocsForRelation(TestRelations.SINGLE);

    assertThat(singleDocs.getCardinality())
        .isEqualTo("1");
  }


  @Test
  public void getVariables_should_handle_simple_template() {

    RhymeRelatedMethodDocs templateDocs = getEntryPointDocsForRelation(TestRelations.TEMPLATE);

    assertThat(templateDocs.getVariables()).hasSize(1);

    RhymeVariableDocs variable = templateDocs.getVariables().get(0);

    assertThat(variable.getName()).isEqualTo("foo");
    assertThat(variable.getType()).isEqualTo("String");
    assertThat(variable.getDescription()).isEqualTo("Javadoc for foo parameter");
  }

  @Test
  public void getVariables_should_keep_order_of_multiple_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.MULTI_VAR_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getName)
        .containsExactly("integer", "collection", "booleanWithoutJavadocs");
  }

  @Test
  public void getVariables_should_handle_missing_javadocs_of_multiple_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.MULTI_VAR_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getDescription)
        .containsExactly("Javadoc comment for integer", "Javadoc comment for collection", "");
  }

  @Test
  public void getVariables_should_extract_types_of_multiple_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.MULTI_VAR_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getType)
        .containsExactly("Integer", "List", "Boolean");
  }

  @Test
  public void getVariables_should_use_alphabetical_order_of_interface_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.INTERFACE_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getName)
        .containsExactly("booleanWithoutJavadocs", "collection", "integer");
  }

  @Test
  public void getVariables_should_handle_missing_javadocs_of_interface_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.INTERFACE_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getDescription)
        .containsExactly("", "Javadoc comment for collection", "Javadoc comment for integer");
  }

  @Test
  public void getVariables_should_extract_types_of_interface_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.INTERFACE_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getType)
        .containsExactly("Boolean", "List", "Integer");
  }

  @Test
  public void getVariables_should_use_original_order_of_class_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.CLASS_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getName)
        .containsExactly("integer", "collection", "booleanWithoutJavadocs");
  }

  @Test
  public void getVariables_should_handle_missing_javadocs_of_class_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.CLASS_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getDescription)
        .containsExactly("Javadoc comment for integer", "Javadoc comment for collection", "");
  }

  @Test
  public void getVariables_should_extract_types_of_class_vars() {

    RhymeRelatedMethodDocs templateDocs = getTestResourceDocsForRelation(TestRelations.CLASS_TEMPLATE);

    assertThat(templateDocs.getVariables())
        .extracting(RhymeVariableDocs::getType)
        .containsExactly("Integer", "List", "Boolean");
  }
}
