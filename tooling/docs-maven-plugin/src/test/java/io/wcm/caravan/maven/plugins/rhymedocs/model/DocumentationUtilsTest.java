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

import static io.wcm.caravan.maven.plugins.rhymedocs.model.DocumentationUtils.findJavaDocForField;
import static io.wcm.caravan.maven.plugins.rhymedocs.model.DocumentationUtils.findJavaDocForMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.File;
import java.util.NoSuchElementException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.library.SortedClassLibraryBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;


public class DocumentationUtilsTest {

  private JavaClass firstClassWithMethod;
  private JavaMethod javaMethod;

  private static JavaProjectBuilder emptyBuilder = new JavaProjectBuilder(new SortedClassLibraryBuilder());

  @BeforeEach
  void setUp() {

    JavaProjectBuilder builder = new JavaProjectBuilder();
    builder.addSourceTree(new File("src/test/java"));

    firstClassWithMethod = builder.getSources().stream()
        .flatMap(source -> source.getClasses().stream())
        .filter(javaClass -> !javaClass.getMethods().isEmpty())
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException());

    javaMethod = firstClassWithMethod.getMethods().get(0);
  }

  @Test
  void getMethod_should_handle_ClassNotFoundException() throws ClassNotFoundException {

    ClassLoader classLoader = Mockito.mock(ClassLoader.class);
    Mockito.when(classLoader.loadClass(ArgumentMatchers.anyString()))
        .thenThrow(new ClassNotFoundException());

    Throwable ex = catchThrowable(() -> DocumentationUtils.getMethod(firstClassWithMethod, javaMethod, classLoader));

    Assertions.assertThat(ex)
        .isInstanceOf(RuntimeException.class)
        .hasMessageStartingWith("Unable to get method");
  }

  @Test
  void findJavaDocForMethod_should_handle_classes_without_sources() throws NoSuchMethodException, SecurityException, NoSuchFieldException {

    String fooDesc = DtoWithJacksonAnnotations.findMethodJavaDoc("getFoo");

    assertThat(fooDesc)
        .isEqualTo("foo property description");

    String barDesc = DtoWithJacksonAnnotations.findMethodJavaDoc("getBar");

    assertThat(barDesc)
        .isEmpty();

    String fieldDesc = DtoWithJacksonAnnotations.findFieldJavaDoc("field");

    assertThat(fieldDesc)
        .isEmpty();
  }

  public static class DtoWithJacksonAnnotations {

    @JsonPropertyDescription("foo property description")
    public String getFoo() {
      return "foo";
    }

    public String getBar() {
      return "bar";
    }

    public String field;

    static String findMethodJavaDoc(String name) throws NoSuchMethodException, SecurityException {

      return findJavaDocForMethod(emptyBuilder, DtoWithJacksonAnnotations.class, DtoWithJacksonAnnotations.class.getMethod(name));
    }

    static String findFieldJavaDoc(String name) throws NoSuchFieldException, SecurityException {

      return findJavaDocForField(emptyBuilder, DtoWithJacksonAnnotations.class, DtoWithJacksonAnnotations.class.getField(name));
    }

  }

  @Test
  void getBeanProperties_should_handle_exceptions() {

    Throwable ex = catchThrowable(() -> DocumentationUtils.getBeanProperties(null));

    assertThat(ex)
        .hasMessageStartingWith("Failed to lookup bean properties for ")
        .hasRootCauseInstanceOf(NullPointerException.class);
  }

  @Test
  void getPublicFields_should_handle_exceptions() {

    Throwable ex = catchThrowable(() -> DocumentationUtils.getPublicFields(null));

    assertThat(ex)
        .hasMessageStartingWith("Failed to lookup fields for ")
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
  }


}
