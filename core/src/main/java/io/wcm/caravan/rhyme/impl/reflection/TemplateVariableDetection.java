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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.FieldUtils;

import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariables;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

public class TemplateVariableDetection {

  public static Map<String, Object> getVariablesNameValueMap(Method method, Optional<Object[]> args) {

    Map<String, Object> map = new LinkedHashMap<>();

    findVariables(method, args)
        .forEach(def -> map.put(def.getName(), def.getValue()));

    return map;
  }

  public static List<Variable> findVariables(Method relatedMethod, Optional<Object[]> methodArgs) {

    Object[] args;
    if (methodArgs.isPresent()) {
      args = methodArgs.get();
    }
    else {
      args = new Object[relatedMethod.getParameterCount()];
    }

    List<Variable> definitions = new ArrayList<>();

    for (int i = 0; i < relatedMethod.getParameterCount(); i++) {
      Parameter parameter = relatedMethod.getParameters()[i];

      TemplateVariables variables = parameter.getAnnotation(TemplateVariables.class);
      TemplateVariable variable = parameter.getAnnotation(TemplateVariable.class);
      if (variables != null) {
        definitions.addAll(getVariableInfos(args[i], parameter.getType()));
      }
      else if (variable != null) {
        definitions.add(new Variable(variable.value(), parameter.getType(), args[i]));
      }
      // if no annotation was used, we can try to extract the parameter name from the method.
      // these arguments will be called "arg0", "arg1" etc if this information was stripped from the compiler
      else if (!("arg" + i).equals(parameter.getName())) {
        definitions.add(new Variable(parameter.getName(), parameter.getType(), args[i]));
      }
      else {
        throw new HalApiDeveloperException("method parameter names have been stripped for  " + relatedMethod + ", so they do need to be annotated with either"
            + " @" + TemplateVariable.class.getSimpleName()
            + " or @" + TemplateVariables.class.getSimpleName());
      }

    }

    return definitions;
  }

  public static class Variable {

    private final String name;
    private final Class type;
    private final Object value;

    private boolean collection;

    private Variable(String name, Class type, Object value) {
      this.name = name;
      this.type = type;
      this.value = value;
    }

    public String getName() {
      return this.name;
    }

    public Class getType() {
      return this.type;
    }

    public Object getValue() {
      return this.value;
    }
  }

  private static List<Variable> getVariableInfos(Object instance, Class dtoClass) {

    if (dtoClass.isInterface()) {
      return getVariableInfosFromPublicGetter(instance, dtoClass);
    }

    return getVariablesInfosFromFields(instance, dtoClass);
  }

  private static <T> List<Variable> getVariableInfosFromPublicGetter(Object instance, Class dtoClass) {
    try {
      return Stream.of(Introspector.getBeanInfo(dtoClass).getPropertyDescriptors())
          .map(property -> {
            Object value = invokeMethod(property.getReadMethod(), instance);
            return new Variable(property.getName(), property.getReadMethod().getReturnType(), value);
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
      return readMethod.invoke(instance, new Object[0]);
    }
    catch (InvocationTargetException | IllegalAccessException | RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to invoke getter " + readMethod.getName() + " from class " + instance.getClass().getSimpleName(), ex);
    }
  }

  private static <T> List<Variable> getVariablesInfosFromFields(Object instance, Class dtoClass) {

    try {
      return Stream.of(FieldUtils.getAllFields(dtoClass))
          .filter(field -> !field.isSynthetic())
          .map(field -> {
            Object value = getFieldValue(field, instance);

            return new Variable(field.getName(), field.getType(), value);
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
