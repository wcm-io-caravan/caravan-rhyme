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
package io.wcm.caravan.rhyme.impl.reflection;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.rhyme.impl.reflection.TemplateVariableDetection.findVariables;
import static io.wcm.caravan.rhyme.impl.reflection.TemplateVariableDetection.getVariablesNameValueMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariables;
import io.wcm.caravan.rhyme.impl.reflection.TemplateVariableDetection.TemplateVariableWithTypeInfo;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;


public class TemplateVariableDetectionTest {

  @HalApiInterface
  public interface ResourceWithTemplate {

    LinkableTestResource linkTemplate(
        @TemplateVariable("withAnnotation") String withAnnotation,
        @TemplateVariables DtoClass dtoClass,
        @TemplateVariables DtoInterface dtoInterace);
  }

  public static class DtoClass {

    public final static String CONSTANT = "constant";

    public Integer fromClass;
  }

  interface DtoInterface {

    Boolean getFromInterface();
  }

  private Method getMethod(String name) throws NoSuchMethodException {
    return ResourceWithTemplate.class.getMethod(name, String.class, DtoClass.class, DtoInterface.class);
  }

  private TemplateVariableWithTypeInfo getVarInfo(String name) throws NoSuchMethodException {

    List<TemplateVariableWithTypeInfo> variables = findVariables(getMethod("linkTemplate"), Optional.empty());

    return variables.stream()
        .filter(var -> name.equals(var.getName()))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("Didn't find info for variable named " + name));
  }

  @Test
  public void testFindVariables_should_handle_var_with_annotation() throws Exception {

    TemplateVariableWithTypeInfo varInfo = getVarInfo("withAnnotation");

    assertThat(varInfo.getType()).isEqualTo(String.class);
    assertThat(varInfo.getDtoClass()).isNull();
    assertThat(varInfo.getDtoField()).isNull();
    assertThat(varInfo.getDtoMethod()).isNull();
  }

  @Test
  public void testFindVariables_should_handle_class_variables() throws Exception {

    TemplateVariableWithTypeInfo varInfo = getVarInfo("fromClass");

    assertThat(varInfo.getType()).isEqualTo(Integer.class);
    assertThat(varInfo.getDtoClass()).isSameAs(DtoClass.class);
    assertThat(varInfo.getDtoField()).extracting(Field::getName).isEqualTo("fromClass");
    assertThat(varInfo.getDtoMethod()).isNull();
  }

  @Test
  public void testFindVariables_should_handle_interface_variables() throws Exception {

    TemplateVariableWithTypeInfo varInfo = getVarInfo("fromInterface");

    assertThat(varInfo.getType()).isEqualTo(Boolean.class);
    assertThat(varInfo.getDtoClass()).isSameAs(DtoInterface.class);
    assertThat(varInfo.getDtoField()).isNull();
    assertThat(varInfo.getDtoMethod()).extracting(Method::getName).isEqualTo("getFromInterface");
  }

  @Test
  public void testFindVariables_should_ignore_static_fields() throws Exception {

    List<TemplateVariableWithTypeInfo> variables = findVariables(getMethod("linkTemplate"), Optional.empty());

    assertThat(variables)
        .extracting(TemplateVariableWithTypeInfo::getName)
        .containsExactly("withAnnotation", "fromClass", "fromInterface");
  }

  @Test
  public void getVariablesNameValueMap_should_return_values_from_arguments() throws Exception {

    String value = "value";

    DtoClass dtoClass = new DtoClass();
    dtoClass.fromClass = 123;

    DtoInterface dtoInterface = new DtoInterface() {

      @Override
      public Boolean getFromInterface() {
        return true;
      }

    };

    Object[] args = new Object[] {
        value, dtoClass, dtoInterface
    };

    Map<String, Object> map = getVariablesNameValueMap(getMethod("linkTemplate"), Optional.of(args));

    assertThat(map.keySet()).containsExactly("withAnnotation", "fromClass", "fromInterface");
    assertThat(map.values()).containsExactly(value, dtoClass.fromClass, dtoInterface.getFromInterface());
  }

  @Test
  public void getVariablesNameValueMap_should_return_null_values_with_empty_arguments() throws Exception {

    Map<String, Object> map = getVariablesNameValueMap(getMethod("linkTemplate"), Optional.empty());

    assertThat(map.keySet()).containsExactly("withAnnotation", "fromClass", "fromInterface");
    assertThat(map.values()).containsOnlyNulls();
  }


  @HalApiInterface
  public interface ResourceWithMissingAnnotations {

    @Related(ITEM)
    LinkableTestResource getItem(String parameter);
  }

}
