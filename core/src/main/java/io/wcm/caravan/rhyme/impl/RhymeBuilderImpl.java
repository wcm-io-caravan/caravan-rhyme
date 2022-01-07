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
package io.wcm.caravan.rhyme.impl;

import java.util.ArrayList;
import java.util.List;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;
import io.wcm.caravan.rhyme.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupportAdapter;

/**
 * Implementation of the {@link RhymeBuilder} interface that allows to configure and create {@link Rhyme} instances
 */
public class RhymeBuilderImpl implements RhymeBuilder {

  private final HalResourceLoader resourceLoader;

  private RhymeDocsSupport rhymeDocsSupport;

  private final List<HalApiTypeSupport> registeredTypeSupports = new ArrayList<>();

  private final List<ExceptionStatusAndLoggingStrategy> exceptionStrategies = new ArrayList<>();

  /**
   * @param resourceLoader to be used to load upstream-resource (or null if not needed)
   */
  public RhymeBuilderImpl(HalResourceLoader resourceLoader) {

    this.resourceLoader = resourceLoader;

    this.registeredTypeSupports.add(new DefaultHalApiTypeSupport());
  }


  @Override
  public RhymeBuilder withRhymeDocsSupport(RhymeDocsSupport rhymeDocsSupport) {

    this.rhymeDocsSupport = rhymeDocsSupport;

    return this;
  }

  @Override
  public RhymeBuilder withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport) {

    registeredTypeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    return this;
  }

  @Override
  public RhymeBuilder withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport) {

    registeredTypeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    return this;
  }

  @Override
  public RhymeBuilder withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy) {

    exceptionStrategies.add(customStrategy);
    return this;
  }

  @Override
  public Rhyme buildForRequestTo(String incomingRequestUri) {

    HalApiTypeSupport typeSupport = RhymeBuilderUtils.getEffectiveTypeSupport(registeredTypeSupports);
    ExceptionStatusAndLoggingStrategy exceptionStrategy = RhymeBuilderUtils.getEffectiveExceptionStrategy(exceptionStrategies);

    return new RhymeImpl(incomingRequestUri, resourceLoader, exceptionStrategy, typeSupport, rhymeDocsSupport);
  }

}
