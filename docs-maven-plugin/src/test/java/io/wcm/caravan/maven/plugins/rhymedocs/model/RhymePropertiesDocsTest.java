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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithFieldProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithFieldProperties.FieldProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRxBeanProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRxBeanProperties.BeanProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestEntryPoint;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;

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
  public void getPropertyNameWithPadding_should_reflect_object_structure() {

    String onePaddingLevel = StringUtils.repeat("&nbsp;", 3);
    String twoPaddingLevels = StringUtils.repeat(onePaddingLevel, 2);

    RhymeResourceDocs docsWithNestedProperties = getDocsFor(ResourceWithNestedProperties.class);

    assertThat(docsWithNestedProperties.getProperties())
        .extracting(RhymePropertyDocs::getPropertyNameWithPadding)
        .containsExactly(
            "bean",
            onePaddingLevel + ".fieldList",
            twoPaddingLevels + "[n].foo",
            onePaddingLevel + ".innerField",
            "beanList",
            onePaddingLevel + "[n]",
            "field");
  }

  @HalApiInterface
  public interface ResourceWithNestedProperties {

    @ResourceState
    Optional<NestedProperties> getState();

    class NestedProperties {

      public InnerBeanProperties bean;

      public InnerFieldProperties field;

      public List<InnerBeanProperties> beanList;

    }

    class InnerBeanProperties {

      private InnerFieldProperties innerField;

      private List<InnerFieldProperties> fieldList;

      public InnerFieldProperties getInnerField() {
        return this.innerField;
      }

      public void setInnerField(InnerFieldProperties innerField) {
        this.innerField = innerField;
      }

      public List<InnerFieldProperties> getFieldList() {
        return this.fieldList;
      }

      public void setFieldList(List<InnerFieldProperties> fieldList) {
        this.fieldList = fieldList;
      }
    }

    class InnerFieldProperties {

      public String foo;
    }

  }


  @Test
  public void getProperties_should_skip_repeated_properties_of_same_object_class() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithRepeatedObjectTypes.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/field1", "/field1/bar", "/field1/foo", "/field1/list", "/field2");

    assertThat(findDocsForProperty("/field2", docs).getDescription())
        .startsWith("Javadoc for field2")
        .endsWith("(an object with same properties as /field1)");
  }

  @HalApiInterface
  public interface ResourceWithRepeatedObjectTypes {

    @ResourceState
    Properties getState();

    class Properties {

      /**
       * Javadoc for field1
       */
      public FieldProperties field1;

      /**
       * Javadoc for field2
       */
      public FieldProperties field2;
    }
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
        .isEqualTo("(an object with same properties as /field1/0)");
  }

  @HalApiInterface
  public interface ResourceWithRepeatedLists {

    @ResourceState
    Properties getState();

    class Properties {

      /**
       * Javadoc for field1
       */
      public List<BeanProperties> field1;

      /**
       * Javadoc for field2
       */
      public List<BeanProperties> field2;
    }
  }

  @Test
  public void getProperties_should_skip_recursive_properties_of_same_object_class() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithRecursiveObjectTypes.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/field1", "/field1/bar", "/field1/foo", "/field1/list", "/field2");

    assertThat(findDocsForProperty("/field2", docs).getDescription())
        .startsWith("Javadoc for field2")
        .endsWith("(an object with same properties as /field1)");
  }

  @HalApiInterface
  public interface ResourceWithRecursiveObjectTypes {

    @ResourceState
    ResourceWithRecursiveObjectTypes.Properties getState();

    class Properties {

      public FieldProperties field1;

      /**
       * Javadoc for field2
       */
      public FieldProperties field2;
    }
  }

  @Test
  public void getProperties_should_return_single_line_for_json_node_state() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithJsonProperties.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/");

    RhymePropertyDocs property = findDocsForProperty("/", docs);

    assertThat(property.getDescription())
        .contains("uses a generic JSON node");

    assertThat(property.getType())
        .isEqualTo("JSON Object");
  }

  @HalApiInterface
  interface ResourceWithJsonProperties {

    @ResourceState
    JsonNode getState();
  }

  @Test
  public void getProperties_should_handle_arrays() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithArrayProperties.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/objectArray",
            "/objectArray/0/bar",
            "/objectArray/0/foo",
            "/objectArray/0/list",
            "/stringArray");

    assertThat(findDocsForProperty("/objectArray", docs).getType())
        .isEqualTo("List<FieldProperties>");

    assertThat(findDocsForProperty("/stringArray", docs).getType())
        .isEqualTo("String[]");
  }

  @HalApiInterface
  interface ResourceWithArrayProperties {

    @ResourceState
    Properties getState();

    class Properties {

      public List<ResourceWithFieldProperties.FieldProperties> objectArray;

      public String[] stringArray;
    }
  }

  @Test
  public void getProperties_should_look_into_jackson_annotations() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithAnnotatedProperties.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly("/annotatedField", "/annotatedMethod");

    assertThat(findDocsForProperty("/annotatedField", docs).getDescription())
        .isEqualTo("field description");

    assertThat(findDocsForProperty("/annotatedMethod", docs).getDescription())
        .isEqualTo("method description");
  }

  @HalApiInterface
  interface ResourceWithAnnotatedProperties {

    @ResourceState
    Properties getState();

    class Properties {

      @JsonPropertyDescription("field description")
      public String annotatedField;

      @JsonPropertyDescription("method description")
      public String getAnnotatedMethod() {
        return "bar";
      }

    }
  }

  @Test
  public void getProperties_should_handle_types_without_source() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithPropertiesWithoutSource.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .contains("/locale", "/locale/country"); // and many more

    assertThat(findDocsForProperty("/locale", docs).getType())
        .isEqualTo("Locale");

    RhymePropertyDocs countryDocs = findDocsForProperty("/locale/country", docs);

    assertThat(countryDocs.getType())
        .isEqualTo("String");

    assertThat(countryDocs.getDescription())
        .isEmpty();
  }

  @HalApiInterface
  interface ResourceWithPropertiesWithoutSource {

    @ResourceState
    Properties getState();

    class Properties {

      public Locale locale;

    }
  }

  @Test
  public void getProperties_should_handle_type_edge_cases() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithEdgeCaseProperties.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly(
            "/intArrayList",
            "/map",
            "/primitiveList",
            "/untypedList");

    assertThat(findDocsForProperty("/intArrayList", docs).getType())
        .isEqualTo("List<int[]>");

    assertThat(findDocsForProperty("/map", docs).getType())
        .isEqualTo("Map<String, String>");

    assertThat(findDocsForProperty("/primitiveList", docs).getType())
        .isEqualTo("List<Boolean>");

    assertThat(findDocsForProperty("/untypedList", docs).getType())
        .isEqualTo("List");
  }

  @HalApiInterface
  interface ResourceWithEdgeCaseProperties {

    @ResourceState
    Properties getState();

    class Properties {

      public List<int[]> intArrayList;

      public Map<String, String> map;

      public List<Boolean> primitiveList;

      public List<?> untypedList;

      public void setFoo(@SuppressWarnings("unused") String foo) {
        // just to check that bean properties without getters are ignored
      }


    }
  }

  @Test
  public void getProperties_should_ignore_fields_and_methods_with_json_ignore() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithJsonIgnoreAnnotation.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly(
            "/foo");
  }

  @HalApiInterface
  interface ResourceWithJsonIgnoreAnnotation {

    @ResourceState
    FieldProperties getState();

    class FieldProperties {

      private String foo;

      @JsonIgnore
      private String jsonIgnoreOnField;

      private String jsonIgnoreOnGetter;

      @JsonIgnore
      public String jsonIgnoreOnPublicField;

      public String getFoo() {
        return this.foo;
      }

      public String getJsonIgnoreOnField() {
        return jsonIgnoreOnField;
      }

      @JsonIgnore
      public String getJsonIgnoreOnGetter() {
        return jsonIgnoreOnGetter;
      }

    }
  }

  @Test
  public void getProperties_should_also_use_javadocs_from_private_fields() {

    RhymeResourceDocs docs = getDocsFor(ResourceWithPrivateFieldsAndBeanProperties.class);

    assertThat(docs.getProperties())
        .extracting(RhymePropertyDocs::getJsonPointer)
        .containsExactly(
            "/javadocInField", "/javadocInGetter");

    assertThat(findDocsForProperty("/javadocInField", docs).getDescription())
        .isEqualTo("Javadoc from field");

    assertThat(findDocsForProperty("/javadocInGetter", docs).getDescription())
        .isEqualTo("Javadoc from getter");
  }

  @HalApiInterface
  interface ResourceWithPrivateFieldsAndBeanProperties {

    @ResourceState
    FieldProperties getState();

    class FieldProperties {

      /**
       * Javadoc from field
       */
      private Integer javadocInField;

      private Integer javadocInGetter;

      public Integer getJavadocInField() {
        return javadocInField;
      }

      /**
       * @return Javadoc from getter
       */
      public Integer getJavadocInGetter() {
        return javadocInGetter;
      }

    }
  }
}
