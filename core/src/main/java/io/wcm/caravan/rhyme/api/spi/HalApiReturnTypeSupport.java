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

import java.util.Optional;
import java.util.function.Function;

import org.osgi.annotation.versioning.ConsumerType;

import io.reactivex.rxjava3.core.Observable;

/**
 * An SPI interface that allows to use additional return types (e.g. RxJava 1 observables, or Spring Reactor Flux or
 * Mono) in your HAL API interfaces
 */
@ConsumerType
public interface HalApiReturnTypeSupport {

  /**
   * @param targetType the type to convert to
   * @return a function that will create an instance of the given type from an {@link Observable},
   *         or null if this is not possible
   * @param <T> the type to covert to
   */
  <T> Function<Observable, T> convertFromObservable(Class<T> targetType);

  /**
   * @param sourceType the type to convert from
   * @return a function that will create an {@link Observable} from an instance of the given type,
   *         or null if this is not possible
   */
  Function<Object, Observable<Object>> convertToObservable(Class<?> sourceType);

  /**
   * @param returnType the return type of an annotated method
   * @return true if the type is a collection or a reactive type that can emit multiple values
   */
  boolean isProviderOfMultiplerValues(Class<?> returnType);

  /**
   * @param returnType the return type of an annotated method
   * @return true if the type is an {@link Optional} or a reactive type that can emit zero or one values
   */
  default boolean isProviderOfOptionalValue(Class<?> returnType) {
    return false;
  }

}
