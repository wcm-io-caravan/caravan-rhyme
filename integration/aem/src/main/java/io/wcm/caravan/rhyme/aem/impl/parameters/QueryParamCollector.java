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
package io.wcm.caravan.rhyme.aem.impl.parameters;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.parameters.QueryParam;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

public class QueryParamCollector {

  public Map<String, Object> getQueryParameters(Object slingModel) {

    Map<String, Object> map = new LinkedHashMap<>();

    addQueryParamFieldsFromModel(slingModel, map);

    handleNestedModels(slingModel, map);

    return map;
  }

  private void addQueryParamFieldsFromModel(Object instance, Map<String, Object> map) {

    List<Field> allFields = FieldUtils.getAllFieldsList(instance.getClass());

    allFields.stream()
        .filter(field -> field.isAnnotationPresent(QueryParam.class))
        .forEach(field -> map.put(field.getName(), readField(instance, field)));
  }

  private void handleNestedModels(Object slingModel, Map<String, Object> map) {

    List<Field> allFields = FieldUtils.getAllFieldsList(slingModel.getClass());

    allFields.stream()
        .filter(field -> field.isAnnotationPresent(Self.class))
        .filter(QueryParamCollector::isSlingRhymeModel)
        .map(field -> readField(slingModel, field))
        .filter(Objects::nonNull)
        .forEach(fieldValue -> addQueryParamFieldsFromModel(fieldValue, map));
  }

  private static boolean isSlingRhymeModel(Field field) {

    Model model = field.getType().getAnnotation(Model.class);

    if (model == null) {
      return false;
    }

    return Stream.of(model.adaptables())
        .anyMatch(clazz -> SlingRhyme.class.isAssignableFrom(clazz));
  }


  static Object readField(Object slingModel, Field field) {
    try {
      return FieldUtils.readField(field, slingModel, true);
    }
    catch (RuntimeException | IllegalAccessException ex) {
      throw new HalApiDeveloperException("Failed to access field " + field + " of  " + slingModel, ex);
    }
  }
}
