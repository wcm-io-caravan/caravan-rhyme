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
package io.wcm.caravan.hal.api.server.impl.reflection;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;

public class JaxRsReflectionUtils {

  public static Map<String, Object> getPathParameterMap(Object resourceImpl) {

    Map<String, Object> parameterMap = new LinkedHashMap<>();

    findFieldsDefinedInResource(resourceImpl).stream()
        .filter(field -> field.getAnnotation(PathParam.class) != null)
        .map(field -> Pair.of(field.getAnnotation(PathParam.class).value(), getFieldValue(field, resourceImpl)))
        .forEach(pair -> parameterMap.put(pair.getKey(), pair.getValue()));

    return parameterMap;
  }

  public static Map<String, Object> getQueryParameterMap(Object resourceImpl) {

    Map<String, Object> parameterMap = new LinkedHashMap<>();

    findFieldsDefinedInResource(resourceImpl).stream()
        .filter(field -> field.getAnnotation(QueryParam.class) != null)
        .map(field -> Pair.of(field.getAnnotation(QueryParam.class).value(), getFieldValue(field, resourceImpl)))
        .forEach(pair -> parameterMap.put(pair.getKey(), pair.getValue()));

    return parameterMap;
  }

  private static List<Field> findFieldsDefinedInResource(Object resource) {

    return FieldUtils.getAllFieldsList(resource.getClass());
  }

  private static Object getFieldValue(Field field, Object instance) {
    try {
      return FieldUtils.readField(field, instance, true);
    }
    catch (IllegalArgumentException | IllegalAccessException ex) {
      throw new RuntimeException("Failed to access field " + field.getName() + " of class " + instance.getClass().getName() + " through reflection", ex);
    }
  }

}
