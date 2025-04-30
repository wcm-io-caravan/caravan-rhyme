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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.FieldUtils;

import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariables;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

/**
 * implements the logic of discovering the names (and values) of all template variables in the signature
 * of a method annotated with {@link Related}. This is used for the implementation of the dynamic client
 * proxies, and for documentation generation with the rhyme-docs-maven-plugin
 */
public final class TemplateVariableDetection {

  private TemplateVariableDetection() {
    // this class contains only static methods
  }

  /**
   * Collect the names of all template variables in the signature of a {@link Related} method,
   * and populates the values from the corresponding arguments to the method call.
   * These arguments can be empty, e.g. if the method
   * doesn't have any parameters or the actual parameters weren't available because it wasn't invoked.
   * @param method annotated with {@link Related}
   * @param methodArgs the values of each argument provided in the method call (can be empty)
   * @return an object with the variable names and keys, and the corresponding values (which can be null)
   */
  public static Map<String, Object> getVariablesNameValueMap(Method method, Optional<Object[]> methodArgs) {

    Map<String, Object> map = new LinkedHashMap<>();

    findVariables(method, methodArgs)
        .forEach(def -> map.put(def.getName(), def.getValue()));

    return map;
  }

  /**
   * Collect all template variables from the signature of a {@link Related} method
   * @param method annotated with {@link Related}
   * @param methodArgs the values of each argument provided in the method call (can be empty)
   * @return a list with one {@link TemplateVariableWithTypeInfo} for each variable in the signature
   */
  public static List<TemplateVariableWithTypeInfo> findVariables(Method method, Optional<Object[]> methodArgs) {

    Object[] args;
    if (methodArgs.isPresent()) {
      args = methodArgs.get();
    }
    else {
      args = new Object[method.getParameterCount()];
    }

    List<TemplateVariableWithTypeInfo> definitions = new ArrayList<>();

    for (int i = 0; i < method.getParameterCount(); i++) {
      Parameter parameter = method.getParameters()[i];

      TemplateVariables variables = parameter.getAnnotation(TemplateVariables.class);
      TemplateVariable variable = parameter.getAnnotation(TemplateVariable.class);
      if (variables != null) {
        definitions.addAll(getVariableInfos(args[i], parameter.getType()));
      }
      else if (variable != null) {
        definitions.add(new TemplateVariableWithTypeInfo(variable.value(), parameter.getType(), args[i]));
      }
      // if no annotation was used, we can try to extract the parameter name from the method.
      // these arguments will be called "arg0", "arg1" etc if this information was stripped from the compiler
      else if (!("arg" + i).equals(parameter.getName())) {
        definitions.add(new TemplateVariableWithTypeInfo(parameter.getName(), parameter.getType(), args[i]));
      }
      else {
        throw new HalApiDeveloperException("method parameter names have been stripped for  " + method + ", so they do need to be annotated with either"
            + " @" + TemplateVariable.class.getSimpleName()
            + " or @" + TemplateVariables.class.getSimpleName());
      }

    }

    return definitions;
  }

  /**
   * Contains name, value (if present) and type information for a template variable in a call to a {@link Related}
   * method
   */
  public static class TemplateVariableWithTypeInfo {

    private final String name;
    private final Class type;
    private final Object value;

    private Class dtoClass;
    private Method dtoMethod;
    private Field dtoField;

    private TemplateVariableWithTypeInfo(String name, Class type, Object value) {
      this.name = name;
      this.type = type;
      this.value = value;
    }

    /**
     * @return the name of the template variable
     */
    public String getName() {
      return this.name;
    }

    /**
     * @return the java type of the template variable
     */
    public Class getType() {
      return this.type;
    }

    /**
     * @return the value of the template variable (can be null)
     */
    public Object getValue() {
      return this.value;
    }

    /**
     * If this variable is defined by a {@link TemplateVariables} parameter, this method will return
     * the class where the corresponding method or field is defined.
     * @return the class/interface of the {@link TemplateVariable} parameter (can be null)
     */
    public Class getDtoClass() {
      return this.dtoClass;
    }

    /**
     * Only if this variable is defined by a get method of a {@link TemplateVariable} class/interface,
     * this method will return the method that needs to be called to get the parameter value
     * @return the method (can be null)
     */
    public Method getDtoMethod() {
      return this.dtoMethod;
    }

    /**
     * Only if this variable is defined by a public field of a {@link TemplateVariable} class/interface,
     * this method will return the field containing the parameter value
     * @return the field (can be null)
     */
    public Field getDtoField() {
      return this.dtoField;
    }
  }

  private static List<TemplateVariableWithTypeInfo> getVariableInfos(Object instance, Class dtoClass) {

    if (dtoClass.isInterface()) {
      return getVariableInfosFromPublicGetter(instance, dtoClass);
    }

    return getVariablesInfosFromFields(instance, dtoClass);
  }

  private static List<TemplateVariableWithTypeInfo> getVariableInfosFromPublicGetter(Object instance, Class dtoClass) {
    try {

      PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(dtoClass).getPropertyDescriptors();
      if (propertyDescriptors.length == 0) {
        String msg = "Not a single getter method following the JavaBeans naming conventions was found in " + dtoClass;
        throw new HalApiDeveloperException(msg);
      }

      return Stream.of(propertyDescriptors)
          .map(property -> {
            Method readMethod = property.getReadMethod();
            Object value = invokeMethod(readMethod, instance);
            TemplateVariableWithTypeInfo variable = new TemplateVariableWithTypeInfo(property.getName(), readMethod.getReturnType(), value);
            variable.dtoClass = dtoClass;
            variable.dtoMethod = readMethod;
            return variable;
          })
          .collect(Collectors.toList());
    }
    catch (IntrospectionException | RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to extract template variables from interface " + dtoClass.getName() + " through reflection", ex);
    }
  }

  private static Object invokeMethod(Method readMethod, Object instance) {

    if (instance == null) {
      return null;
    }

    try {
      return readMethod.invoke(instance);
    }
    catch (InvocationTargetException | IllegalAccessException | RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to invoke getter " + readMethod.getName() + " from class " + instance.getClass().getSimpleName(), ex);
    }
  }

  private static List<TemplateVariableWithTypeInfo> getVariablesInfosFromFields(Object instance, Class dtoClass) {

    try {
      return Stream.of(FieldUtils.getAllFields(dtoClass))
          .filter(field -> !field.isSynthetic())
          .filter(field -> !Modifier.isStatic(field.getModifiers()))
          .map(field -> {
            Object value = getFieldValue(field, instance);
            TemplateVariableWithTypeInfo variable = new TemplateVariableWithTypeInfo(field.getName(), field.getType(), value);
            variable.dtoClass = dtoClass;
            variable.dtoField = field;
            return variable;
          })
          .collect(Collectors.toList());
    }
    catch (RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to extract template variables from class " + dtoClass.getName() + " through reflection", ex);
    }
  }

  private static Object getFieldValue(Field field, Object instance) {

    if (instance == null) {
      return null;
    }

    try {
      return FieldUtils.readField(field, instance, false);
    }
    catch (IllegalAccessException | RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to read value of field " + field.getName() + " from class " + instance.getClass().getSimpleName()
          + ". Make sure that all fields in your classes used as parameters annotated with @" + TemplateVariables.class.getSimpleName() + " are public", ex);
    }
  }

}
