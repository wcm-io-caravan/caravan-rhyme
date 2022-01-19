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
package io.wcm.caravan.rhyme.jaxrs.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;


class ReflectionUtilsTest {

  @Test
  void testGetFieldValue_with_existing_field() throws NoSuchFieldException {

    ClassWithPrivateField instance = new ClassWithPrivateField();
    instance.intField = 123;

    Object value = ReflectionUtils.getFieldValue(ClassWithPrivateField.getIntField(), instance);

    assertThat(value).isEqualTo(instance.intField);
  }

  @Test
  void testGetFieldValue_with_null_field() {

    ClassWithPrivateField instance = new ClassWithPrivateField();

    Throwable ex = catchThrowable(() -> ReflectionUtils.getFieldValue(null, instance));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Null field given");
  }

  @Test
  void testGetFieldValue_with_invalid_field() {

    ClassWithPrivateField instance = new ClassWithPrivateField();

    Throwable ex = catchThrowable(() -> ReflectionUtils.getFieldValue(OtherClassWithPrivateField.getStringField(), instance));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to access field stringField");

  }

  static class ClassWithPrivateField {

    private static Field getIntField() throws NoSuchFieldException {
      return ClassWithPrivateField.class.getDeclaredField("intField");
    }

    private int intField;
  }

  static class OtherClassWithPrivateField {

    private static Field getStringField() throws NoSuchFieldException {
      return OtherClassWithPrivateField.class.getDeclaredField("stringField");
    }

    @SuppressWarnings("unused")
    private String stringField;
  }
}
