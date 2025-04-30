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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.collect.ImmutableList;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaType;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

final class DocumentationUtils {

  private DocumentationUtils() {
    // only static methods
  }

  static Stream<JavaMethod> getMethodsWithAnnotation(JavaClass apiInterface, Class<? extends Annotation> annotationClazz) {

    return apiInterface.getMethods(true).stream()
        .filter(method -> hasAnnotation(method, annotationClazz));
  }

  static boolean hasAnnotation(JavaAnnotatedElement element, Class<? extends Annotation> annotationClazz) {

    return element.getAnnotations().stream()
        .anyMatch(item -> item.getType().isA(annotationClazz.getName()));
  }

  static Method getMethod(JavaClass javaClazz, JavaMethod javaMethod, ClassLoader classLoader) {
    try {
      Class<?> clazz = loadClass(javaClazz, classLoader);

      Class[] paramTypes = javaMethod.getParameterTypes().stream()
          .map(paramClazz -> loadClass(paramClazz, classLoader))
          .toArray(Class[]::new);

      return clazz.getMethod(javaMethod.getName(), paramTypes);
    }
    catch (RuntimeException | NoSuchMethodException ex) {
      throw new HalApiDeveloperException("Unable to get method '" + javaClazz.getName() + "#" + javaMethod.getName(), ex);
    }
  }

  private static Class<?> loadClass(JavaType javaType, ClassLoader classLoader) {
    try {
      return classLoader.loadClass(javaType.getBinaryName());
    }
    catch (ClassNotFoundException ex) {
      throw new HalApiDeveloperException("Failed to load class " + javaType.getFullyQualifiedName(), ex);
    }
  }

  static String findJavaDocForMethod(JavaProjectBuilder builder, Class dtoClass, Method dtoMethod) {

    JavaClass javaClass = builder.getClassByName(dtoClass.getName());

    JavaMethod javaMethod = javaClass.getMethods().stream()
        .filter(m -> m.getName().equals(dtoMethod.getName()))
        .findFirst()
        .orElse(null);

    if (javaMethod == null) {
      JsonPropertyDescription propertyDesc = dtoMethod.getAnnotation(JsonPropertyDescription.class);
      if (propertyDesc != null) {
        return propertyDesc.value();
      }
    }
    else {

      String javaDoc = getJavaDocCommentOrReturnTag(javaMethod);
      if (StringUtils.isNotBlank(javaDoc)) {
        return javaDoc;
      }

      JsonPropertyDescription propertyDesc = dtoMethod.getAnnotation(JsonPropertyDescription.class);
      if (propertyDesc != null) {
        return propertyDesc.value();
      }
    }

    return "";
  }

  static String getJavaDocCommentOrReturnTag(JavaMethod javaMethod) {

    String comment = javaMethod.getComment();
    if (StringUtils.isNotBlank(comment)) {
      return removeLinks(comment);
    }

    DocletTag returnTag = javaMethod.getTagByName("return", true);
    if (returnTag != null) {
      return removeLinks(returnTag.getValue());
    }

    return "";
  }

  public static String findJavaDocForField(JavaProjectBuilder builder, Class stateType, String name) {

    return FieldUtils.getAllFieldsList(stateType).stream()
        .filter(field -> field.getName().equals(name))
        .map(field -> findJavaDocForField(builder, stateType, field))
        .findFirst()
        .orElse("");
  }

  static String findJavaDocForField(JavaProjectBuilder builder, Class dtoClass, Field dtoField) {

    JavaClass javaClass = builder.getClassByName(dtoClass.getName());

    JavaField javaField = javaClass.getFieldByName(dtoField.getName());
    if (javaField == null) {
      return "";
    }

    String desc = StringUtils.trimToEmpty(javaField.getComment());
    if (desc.isEmpty()) {
      JsonPropertyDescription jsonDesc = dtoField.getAnnotation(JsonPropertyDescription.class);
      if (jsonDesc != null) {
        return jsonDesc.value();
      }
    }

    return removeLinks(desc);
  }

  static Stream<PropertyDescriptor> getBeanProperties(Class<?> type) {

    List<String> propertyDenyList = ImmutableList.of("class", "declaringClass");

    try {
      return Stream.of(Introspector.getBeanInfo(type).getPropertyDescriptors())
          .filter(property -> property.getReadMethod() != null)
          .filter(property -> !propertyDenyList.contains(property.getName()))
          .filter(property -> !hasJsonIgnoreAnnotation(type, property));
    }
    catch (IntrospectionException | RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to lookup bean properties for " + type, ex);
    }
  }

  private static boolean hasJsonIgnoreAnnotation(Class<?> type, PropertyDescriptor property) {

    if (property.getReadMethod().getAnnotation(JsonIgnore.class) != null) {
      return true;
    }

    // the JsonIgnore annotation can also be attached to a field with the same name as the property
    return Stream.of(FieldUtils.getAllFields(type))
        .filter(field -> field.getName().equals(property.getName()))
        .anyMatch(field -> field.getAnnotation(JsonIgnore.class) != null);
  }

  static Stream<Field> getPublicFields(Class<?> type) {
    try {
      return Stream.of(FieldUtils.getAllFields(type))
          .filter(field -> Modifier.isPublic(field.getModifiers()))
          .filter(field -> !Modifier.isStatic(field.getModifiers()))
          .filter(field -> field.getAnnotation(JsonIgnore.class) == null);
    }
    catch (RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to lookup fields for " + type, ex);
    }
  }

  public static String removeLinks(String javadoc) {

    if (javadoc == null) {
      return null;
    }

    return javadoc.replaceAll("\\{\\@link (.*?)\\}", "$1");
  }


}
