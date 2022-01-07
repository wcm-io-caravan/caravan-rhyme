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
package io.wcm.caravan.rhyme.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.impl.reflection.CompositeHalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.renderer.CompositeExceptionStatusAndLoggingStrategy;

public class RhymeBuilderUtils {

  public static ExceptionStatusAndLoggingStrategy getEffectiveExceptionStrategy(List<ExceptionStatusAndLoggingStrategy> exceptionStrategies) {

    List<ExceptionStatusAndLoggingStrategy> nonNullStrategies = exceptionStrategies.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    if (nonNullStrategies.isEmpty()) {
      return null;
    }
    if (nonNullStrategies.size() == 1) {
      return nonNullStrategies.get(0);
    }

    return new CompositeExceptionStatusAndLoggingStrategy(nonNullStrategies);
  }

  public static HalApiTypeSupport getEffectiveTypeSupport(List<HalApiTypeSupport> registeredTypeSupports) {

    if (registeredTypeSupports.isEmpty()) {
      return new DefaultHalApiTypeSupport();
    }
    if (registeredTypeSupports.size() == 1) {
      return registeredTypeSupports.get(0);
    }

    return new CompositeHalApiTypeSupport(registeredTypeSupports);
  }

}
