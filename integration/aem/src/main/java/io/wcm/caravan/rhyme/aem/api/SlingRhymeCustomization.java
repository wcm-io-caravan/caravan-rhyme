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
package io.wcm.caravan.rhyme.aem.api;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.annotation.versioning.ConsumerType;

import io.wcm.caravan.rhyme.aem.impl.HalApiServlet;
import io.wcm.caravan.rhyme.aem.impl.SlingRhymeCustomizationManager;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;

/**
 * An SPI interface to be implemented as an OSGi component if your application needs to apply further customizations
 * to {@link RhymeBuilder} instance created by the {@link HalApiServlet}. Your implementation will be picked up by the
 * {@link SlingRhymeCustomizationManager}
 */
@ConsumerType
public interface SlingRhymeCustomization {

  /**
   * An extension point that allows to configure the {@link RhymeBuilder} that is used when creating {@link SlingRhyme}
   * instance
   * @param rhymeBuilder the builder used to create the {@link Rhyme} instance for the incoming request
   * @param request which you can inspect to determine whether your customization should be applied
   */
  void configureRhymeBuilder(RhymeBuilder rhymeBuilder, SlingHttpServletRequest request);
}
