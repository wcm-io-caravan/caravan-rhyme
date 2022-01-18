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
package io.wcm.caravan.rhyme.api.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Observable;


class HalApiReturnTypeSupportTest {

  @Test
  void isProviderOfOptionalValue_should_have_default_implementation()  {

    HalApiReturnTypeSupport returnTypeSupport = new HalApiReturnTypeSupport() {

      @Override
      public boolean isProviderOfMultiplerValues(Class<?> returnType) {
        return false;
      }

      @Override
      public Function<Object, Observable<Object>> convertToObservable(Class<?> sourceType) {
        return null;
      }

      @Override
      public <T> Function<Observable, T> convertFromObservable(Class<T> targetType) {
        return null;
      }
    };

    assertThat(returnTypeSupport.isProviderOfOptionalValue(Optional.class))
        .isFalse();
  }
}
