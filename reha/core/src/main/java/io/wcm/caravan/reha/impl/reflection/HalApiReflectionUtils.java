/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.reha.impl.reflection;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.google.common.collect.Lists;

import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.annotations.TemplateVariables;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.reha.api.spi.HalApiAnnotationSupport;

/**
 * Utility methods to inspect method signatures
 */
public final class HalApiReflectionUtils {

  private HalApiReflectionUtils() {
    // static methods only
  }

  static Set<Class<?>> collectInterfaces(Class clazz) {

    return new InterfaceCollector()
        .collectFromClassAndAllSuperClasses(clazz);
  }

  private static final class InterfaceCollector {

    private final Set<Class<?>> collected = new LinkedHashSet<>();

    Set<Class<?>> collectFromClassAndAllSuperClasses(Class clazz) {

      Class c = clazz;
      while (c != null) {
        addInterfacesImplementedBy(c);
        c = c.getSuperclass();
      }

      return collected;
    }

    private void addInterfacesImplementedBy(Class<?> clazz) {
      ArrayList<Class<?>> interfaces = Lists.newArrayList(clazz.getInterfaces());

      interfaces.forEach(this.collected::add);

      // each interface can extend other interfaces
      interfaces.forEach(this::addInterfacesImplementedBy);
    }
  }

  /**
   * Checks which of the interfaces implemented by the given implementation instance is the one which is annotated with
   * {@link HalApiInterface}
   * @param resourceImplInstance an instance of a class implementing a HAL API interface
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return the interface that is annotated with {@link HalApiInterface}
   */
  public static Class<?> findHalApiInterface(Object resourceImplInstance, HalApiAnnotationSupport annotationSupport) {

    Class<?> halApiInterface = collectInterfaces(resourceImplInstance.getClass()).stream()
        .filter(annotationSupport::isHalApiInterface)
        .findFirst()
        .orElseThrow(
            () -> new HalApiDeveloperException(
                "None of the interfaces implemented by the given class " + resourceImplInstance.getClass().getName() + " has a @"
                    + HalApiInterface.class.getSimpleName() + " annotation"));

    if (!Modifier.isPublic(halApiInterface.getModifiers())) {
      throw new HalApiDeveloperException(
          "The interface " + halApiInterface.getName() + " is annotated with @HalApiInterface but it also has to be public");
    }

    return halApiInterface;
  }

  /**
   * @param relatedResourceType an interface used as emission type of a reactive stream
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return true if the given interface is (or extends another interface) annotated with {@link HalApiInterface}
   */
  public static boolean isHalApiInterface(Class<?> relatedResourceType, HalApiAnnotationSupport annotationSupport) {

    if (!relatedResourceType.isInterface()) {
      return false;
    }

    if (annotationSupport.isHalApiInterface(relatedResourceType)) {
      return true;
    }

    return collectInterfaces(relatedResourceType).stream()
        .anyMatch(annotationSupport::isHalApiInterface);
  }

  /**
   * @param apiInterface an interface annotated with {@link HalApiInterface} (either directly or by extending)
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return the method annotated with {@link ResourceState}
   */
  public static Optional<Method> findResourceStateMethod(Class<?> apiInterface, HalApiAnnotationSupport annotationSupport) {

    return Stream.of(apiInterface.getMethods())
        .filter(annotationSupport::isResourceStateMethod)
        .findFirst();
  }

  /**
   * @param apiInterface an interface annotated with {@link HalApiInterface} (either directly or by extending)
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return a list of all methods annotated with {@link RelatedResource}
   */
  public static List<Method> getSortedRelatedResourceMethods(Class<?> apiInterface, HalApiAnnotationSupport annotationSupport) {

    MethodRelationComparator comparator = new MethodRelationComparator(annotationSupport);

    return Stream.of(apiInterface.getMethods())
        .filter(annotationSupport::isRelatedResourceMethod)
        .sorted(comparator)
        .collect(Collectors.toList());
  }

  /**
   * @param dto the DTO objet from which to extract the template variables
   * @param dtoClass the type of the object that was used in the parameter definition
   * @return a map with the names and values of all fields in the given object
   */
  public static Map<String, Object> getTemplateVariablesFrom(Object dto, Class dtoClass) {

    if (dtoClass.isInterface()) {
      return getPublicGetterValuesAsMap(dto, dtoClass);
    }

    return getFieldValuesAsMap(dto, dtoClass);
  }

  private static Map<String, Object> getPublicGetterValuesAsMap(Object instance, Class dtoClass) {
    try {
      Map<String, Object> map = new LinkedHashMap<>();
      for (PropertyDescriptor property : Introspector.getBeanInfo(dtoClass).getPropertyDescriptors()) {
        Object value = instance != null ? property.getReadMethod().invoke(instance, new Object[0]) : null;
        map.put(property.getName(), value);
      }
      return map;
    }
    catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
      throw new HalApiDeveloperException("Failed to extract template variables from class " + dtoClass.getName() + " through reflection", ex);
    }
  }

  private static Map<String, Object> getFieldValuesAsMap(Object instance, Class dtoClass) {

    Map<String, Object> map = new LinkedHashMap<>();

    for (Field field : FieldUtils.getAllFields(dtoClass)) {
      if (!field.isSynthetic()) {
        Object value = instance != null ? getFieldValue(field, instance) : null;
        map.put(field.getName(), value);
      }
    }

    return map;
  }

  private static Object getFieldValue(Field field, Object instance) {
    try {
      return FieldUtils.readField(field, instance, false);
    }
    catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new HalApiDeveloperException("Failed to read value of field " + field.getName() + " from class " + instance.getClass().getSimpleName()
          + ". Make sure that all fields in your classes used as parameters annotated with @" + TemplateVariables.class.getSimpleName() + " are public", ex);
    }
  }

  public static String getSimpleClassName(Object resourceImplInstance, HalApiAnnotationSupport annotationSupport) {

    Class<?> implClass = resourceImplInstance.getClass();

    if (!implClass.isAnonymousClass()) {
      return implClass.getSimpleName();
    }

    try {
      Class<?> apiInterface = findHalApiInterface(resourceImplInstance, annotationSupport);

      return "anonymous " + apiInterface.getSimpleName() + " (defined in " + implClass.getEnclosingClass().getSimpleName() + ")";
    }
    catch (HalApiDeveloperException ex) {
      return implClass.getName();
    }
  }

  /**
   * @param instance of any object
   * @param method a method of this class
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return a string with the simple class name and method name
   */
  public static String getClassAndMethodName(Object instance, Method method, HalApiAnnotationSupport annotationSupport) {

    String simpleClassName = getSimpleClassName(instance, annotationSupport);
    String methodName = method.getName();

    return "#" + methodName + " of " + simpleClassName;
  }

  private static class MethodRelationComparator implements Comparator<Method> {

    private final HalApiAnnotationSupport annotationSupport;

    private MethodRelationComparator(HalApiAnnotationSupport annotationSupport) {
      this.annotationSupport = annotationSupport;
    }

    @Override
    public int compare(Method method1, Method method2) {
      String curi1 = annotationSupport.getRelation(method1);
      String curi2 = annotationSupport.getRelation(method2);

      // make sure that all standard link relations always come before custom relations
      if (curi2.contains(":") && !curi1.contains(":")) {
        return -1;
      }
      if (curi1.contains(":") && !curi2.contains(":")) {
        return 1;
      }

      // otherwise the links should be sorted alphabetically
      return curi1.compareTo(curi2);
    }

  }
}
