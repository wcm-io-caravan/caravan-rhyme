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
package io.wcm.caravan.rhyme.jaxrs.impl;

import java.lang.reflect.Field;

import org.apache.commons.lang3.reflect.FieldUtils;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

final class ReflectionUtils {

  private ReflectionUtils() {
    // static methods only
  }

  static Object getFieldValue(Field field, Object instance) {
    if (instance == null) {
      return null;
    }
    if (field == null) {
      throw new HalApiDeveloperException("Null field given when trying to extract field value from " + instance.getClass().getName() + " instance");
    }
    try {
      return FieldUtils.readField(field, instance, true);
    }
    // CHECKSTYLE:OFF - we really want to catch any possible runtime exception that might be thrown here
    catch (IllegalAccessException | RuntimeException ex) {
      // CHECKSTYLE:ON

      throw new HalApiDeveloperException("Failed to access field " + field.getName() + " of class " + instance.getClass().getName() + " through reflection",
          ex);
    }
  }
}
