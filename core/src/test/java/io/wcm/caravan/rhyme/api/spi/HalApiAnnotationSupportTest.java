/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
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
package io.wcm.caravan.rhyme.api.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class HalApiAnnotationSupportTest {

  private final HalApiAnnotationSupport annotationSupport = new HalApiAnnotationSupport() {

    @Override
    public boolean isHalApiInterface(Class<?> type) {
      return false;
    }

    @Override
    public String getContentType(Class<?> halApiInterface) {
      return null;
    }

    @Override
    public boolean isResourceLinkMethod(Method method) {
      return false;
    }

    @Override
    public boolean isResourceRepresentationMethod(Method method) {
      return false;
    }

    @Override
    public boolean isRelatedResourceMethod(Method method) {
      return false;
    }

    @Override
    public boolean isResourceStateMethod(Method method) {
      return false;
    }

    @Override
    public String getRelation(Method method) {
      return null;
    }

  };

  @Test
  void should_have_default_implementation_for_isResourcePropertyMethod() {

    assertThat(annotationSupport.isResourcePropertyMethod(null))
        .isFalse();
  }

  @Test
  void should_have_default_implementation_for_getProeprtyName() {

    assertThat(annotationSupport.getPropertyName(null))
        .isNull();
  }
}
