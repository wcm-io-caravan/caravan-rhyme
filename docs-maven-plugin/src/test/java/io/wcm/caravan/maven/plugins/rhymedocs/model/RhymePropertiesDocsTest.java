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

import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithFieldProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithNestedProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRecursiveObjectTypes;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRepeatedLists;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRepeatedObjectTypes;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRxBeanProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestEntryPoint;

public class RhymePropertiesDocsTest {

  private RhymePropertyDocs findDocsForProperty(String jsonPointer, RhymeResourceDocs resourceDocs) {

    return resourceDocs.getProperties().stream()
        .filter(docs -> (jsonPointer).equals(docs.getJsonPointer()))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("No documentation found for property " + jsonPointer));
  }

  private RhymePropertyDocs getDocsForBeanProperty(String property) {

    RhymeResourceDocs resourceDocs = getDocsFor(ResourceWithRxBeanProperties.class);

    return findDocsForProperty("/" + property, resourceDocs);
  }

  private RhymePropertyDocs getDocsForFieldProperty(String property) {

    RhymeResourceDocs resourceDocs = getDocsFor(ResourceWithFieldProperties.class);

    return findDocsForProperty("/" + property, resourceDocs);
  }

  @Test
  public void RhymeResourceDocs_getProperties_should_return_empty_list_if_no_ResourceState_is_present() {

    RhymeResourceDocs docsWithoutState = getDocsFor(RhymeDocTestEntryPoint.class);

    assertThat(docsWithoutState.getProperties())
        .isEmpty();
  }

  @Test
  public void RhymeResourceDocs_getProperties_should_return_one_item_for_each_bean_property() {

    RhymeResourceDocs docsWithBeanProperties = getDocsFor(ResourceWithRxBeanProperties.class);

    assertThat(docsWithBeanProperties.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/bar", "/foo", "/list");
  }

  @Test
  public void getType_should_use_return_type_for_bean_properties() {

    RhymePropertyDocs docs = getDocsForBeanProperty("foo");

    assertThat(docs.getType())
        .isEqualTo("String");
  }

  @Test
  public void getType_should_include_generic_parameters_for_bean_property() {

    RhymePropertyDocs docs = getDocsForBeanProperty("list");

    assertThat(docs.getType())
        .isEqualTo("List<Boolean>");
  }

  @Test
  public void getDescription_should_return_empty_string_if_javadocs_are_missing() {

    RhymePropertyDocs docs = getDocsForBeanProperty("foo");

    assertThat(docs.getDescription())
        .isEmpty();
  }

  @Test
  public void getDescription_should_use_javadoc_comment_for_bean_properties() {

    RhymePropertyDocs docs = getDocsForBeanProperty("bar");

    assertThat(docs.getDescription())
        .isEqualTo("Javadoc for #getBar()");
  }


  @Test
  public void getDescription_should_fall_back_to_javadoc_return_tag_for_bean_properties() {

    RhymePropertyDocs docs = getDocsForBeanProperty("list");

    assertThat(docs.getDescription())
        .isEqualTo("a list of boolean flags");
  }

  @Test
  public void RhymeResourceDocs_getProperties_should_return_one_item_for_each_field_property() {

    RhymeResourceDocs docsWithBeanProperties = getDocsFor(ResourceWithFieldProperties.class);

    assertThat(docsWithBeanProperties.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/bar", "/foo", "/list");
  }

  @Test
  public void getType_should_use_return_type_for_field_properties() {

    RhymePropertyDocs docs = getDocsForFieldProperty("foo");

    assertThat(docs.getType())
        .isEqualTo("String");
  }

  @Test
  public void getType_should_include_generic_parameters_for_field() {

    RhymePropertyDocs docs = getDocsForFieldProperty("list");

    assertThat(docs.getType())
        .isEqualTo("List<Boolean>");
  }

  @Test
  public void getDescription_should_return_empty_string_if_field_javadocs_are_missing() {

    RhymePropertyDocs docs = getDocsForFieldProperty("foo");

    assertThat(docs.getDescription())
        .isEmpty();
  }

  @Test
  public void getDescription_should_use_javadoc_comment_for_field_properties() {

    RhymePropertyDocs docs = getDocsForFieldProperty("bar");

    assertThat(docs.getDescription())
        .isEqualTo("Javadoc for bar");
  }

  @Test
  public void RhymeResourceDocs_getProperties_should_recurse_for_arrays_and_objects() {

    RhymeResourceDocs docsWithNestedProperties = getDocsFor(ResourceWithNestedProperties.class);

    assertThat(docsWithNestedProperties.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/bean",
            "/bean/fieldList", "/bean/fieldList/0/foo",
            "/bean/innerField", "/beanList", "/beanList/0",
            "/field");
  }

  @Test
  public void getProperties_should_skip_repeated_properties_of_same_object_class() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithRepeatedObjectTypes.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/field1", "/field1/bar", "/field1/foo", "/field1/list", "/field2");

    assertThat(findDocsForProperty("/field2", docs).getDescription())
        .startsWith("Javadoc for field2")
        .endsWith("(with same properties as /field1)");
  }

  @Test
  public void getProperties_should_skip_repeated_properties_of_same_object_classes_in_lists() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithRepeatedLists.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/field1", "/field1/0/bar", "/field1/0/foo", "/field1/0/list",
            "/field2", "/field2/0");

    assertThat(findDocsForProperty("/field2", docs).getDescription())
        .isEqualTo("Javadoc for field2");

    assertThat(findDocsForProperty("/field2/0", docs).getDescription())
        .isEqualTo("(with same properties as /field1/0)");
  }

  @Test
  public void getProperties_should_skip_recursive_properties_of_same_object_class() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithRecursiveObjectTypes.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/field1", "/field1/bar", "/field1/foo", "/field1/list", "/field2");

    assertThat(findDocsForProperty("/field2", docs).getDescription())
        .startsWith("Javadoc for field2")
        .endsWith("(with same properties as /field1)");
  }
}
