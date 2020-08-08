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
package io.wcm.caravan.reha.jaxrs.impl;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Stream;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.reflect.FieldUtils;

final class JaxRsReflectionUtils {

  private JaxRsReflectionUtils() {
    // static methods only
  }

  static Stream<Field> findFieldsThatContainOtherParamsIn(Class clazz) {

    return findFieldsDefinedInClass(clazz).stream()
        .filter(field -> {
          return findFieldsDefinedInClass(field.getType()).stream()
              .anyMatch(nestedField -> nestedField.getAnnotation(QueryParam.class) != null || nestedField.getAnnotation(PathParam.class) != null);
        });
  }

  static List<Field> findFieldsDefinedInClass(Class clazz) {
    return FieldUtils.getAllFieldsList(clazz);
  }

  static Field getField(Class clazz, String name) {
    return FieldUtils.getField(clazz, name, true);
  }

  static Object getFieldValue(String name, Object instance) {
    if (instance == null) {
      return null;
    }
    Class clazz = instance.getClass();
    Field field = getField(clazz, name);
    return getFieldValue(field, instance);
  }

  static Object getFieldValue(Field field, Object instance) {
    if (instance == null) {
      return null;
    }
    try {
      return FieldUtils.readField(field, instance, true);
    }
    catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException("Failed to access field " + field.getName() + " of class " + instance.getClass().getName() + " through reflection", ex);
    }
  }
}
