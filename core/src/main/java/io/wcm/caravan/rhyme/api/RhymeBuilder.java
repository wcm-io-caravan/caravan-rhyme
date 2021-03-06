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
package io.wcm.caravan.rhyme.api;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.RhymeBuilderImpl;

/**
 * A builder to configure and create a {@link Rhyme} instance to be used throughout the lifecycle of the incoming
 * request.
 */
@ProviderType
public interface RhymeBuilder {

  /**
   * Create a {@link RhymeBuilder} that can only build {@link Rhyme} instances which do not request any resources
   * from upstream services
   * @return the new instance
   */
  static RhymeBuilder withoutResourceLoader() {
    return new RhymeBuilderImpl(null);
  }

  /**
   * Create a {@link RhymeBuilder} to build {@link Rhyme} instances that use the given {@link HalResourceLoader}
   * @param resourceLoader to load resources from upstream services
   * @return the new instance
   */
  static RhymeBuilder withResourceLoader(HalResourceLoader resourceLoader) {
    return new RhymeBuilderImpl(resourceLoader);
  }

  /**
   * Extend the core framework to support additional return types in your annotated HAL API interfaces.
   * You can call this method multiple times if you want to register more than one extension.
   * @param additionalTypeSupport extension to the default type support
   * @return this
   */
  RhymeBuilder withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport);

  /**
   * Extend the core framework to support additional annotation types in your annotated HAL API interfaces.
   * You can call this method multiple times if you want to register more than one extension.
   * @param additionalTypeSupport extension to the default type support
   * @return this
   */
  RhymeBuilder withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport);

  /**
   * Allow the exception handling of the core framework to support additional platform / framework specific exceptions.
   * You can call this method multiple times if you want to register more than one extension.
   * @param customStrategy extension to the default exception handling.
   * @return this
   */
  RhymeBuilder withExceptionStrategy(ExceptionStatusAndLoggingStrategy customStrategy);

  /**
   * Create the {@link Rhyme} instance to be used to throughout the lifecycle of an incoming request
   * @param incomingRequestUri URI of the incoming request (can be absolute or relative, depending on the
   *          platform/framework)
   * @return the new instance
   */
  Rhyme buildForRequestTo(String incomingRequestUri);

}
