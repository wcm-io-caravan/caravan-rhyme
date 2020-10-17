/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.caravan.reha.impl;

import java.util.ArrayList;
import java.util.List;

import io.wcm.caravan.reha.api.Reha;
import io.wcm.caravan.reha.api.RehaBuilder;
import io.wcm.caravan.reha.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.reha.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.reha.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.reha.api.spi.JsonResourceLoader;
import io.wcm.caravan.reha.impl.reflection.CompositeHalApiTypeSupport;
import io.wcm.caravan.reha.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.reha.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.reha.impl.reflection.HalApiTypeSupportAdapter;
import io.wcm.caravan.reha.impl.renderer.CompositeExceptionStatusAndLoggingStrategy;

public class RehaBuilderImpl implements RehaBuilder {

  private final JsonResourceLoader jsonLoader;
  private final List<HalApiTypeSupport> registeredTypeSupports = new ArrayList<>();

  private final List<ExceptionStatusAndLoggingStrategy> exceptionStrategies = new ArrayList<>();

  public RehaBuilderImpl(JsonResourceLoader jsonLoader) {

    this.jsonLoader = jsonLoader;

    this.registeredTypeSupports.add(new DefaultHalApiTypeSupport());
  }

  @Override
  public RehaBuilder withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport) {

    registeredTypeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    return this;
  }

  @Override
  public RehaBuilder withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport) {

    registeredTypeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    return this;
  }

  private HalApiTypeSupport getEffectiveTypeSupport() {

    if (registeredTypeSupports.size() == 1) {
      return registeredTypeSupports.get(0);
    }

    return new CompositeHalApiTypeSupport(registeredTypeSupports);
  }

  @Override
  public RehaBuilder withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy) {

    exceptionStrategies.add(customStrategy);
    return this;
  }

  private ExceptionStatusAndLoggingStrategy getEffectiveExceptionStrategy() {

    if (exceptionStrategies.isEmpty()) {
      return null;
    }
    if (exceptionStrategies.size() == 1) {
      return exceptionStrategies.get(0);
    }

    return new CompositeExceptionStatusAndLoggingStrategy(exceptionStrategies);
  }

  @Override
  public Reha buildForRequestTo(String incomingRequestUri) {

    HalApiTypeSupport typeSupport = getEffectiveTypeSupport();
    ExceptionStatusAndLoggingStrategy exceptionStrategy = getEffectiveExceptionStrategy();

    return new RehaImpl(incomingRequestUri, jsonLoader, exceptionStrategy, typeSupport);
  }

}
