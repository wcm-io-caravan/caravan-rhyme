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
package io.wcm.caravan.rhyme.aem.api;

import java.util.Map;
import java.util.Optional;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.component.annotations.Component;

import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * An SPI interface that needs to be implemented as an OSGi {@link Component} in your service bundle.
 * It is required for the Rhyme AEM integration to know how to find the right resource implementation
 * based on the selector of the incoming request, and how to render all links to these resource
 * classes with the appropriate selector.
 */
@ConsumerType
public interface RhymeResourceRegistration {

  /**
   * Lists all <b>linkable</b> resource implementation classes (e.g. sling models) defined in your bundle, and defines
   * the Sling selectors to be used in the links to these resources
   * @return a {@link Map} with resource implementation sling model classes as keys, and the corresponding selectors as
   *         values
   */
  Map<Class<? extends LinkableResource>, String> getModelClassesWithSelectors();

  /**
   * Returns an instance of the resource that should be linked from the global API discovery entry point of the
   * Rhyme AEM integration. Your API should have one entry point that allows to access / discover all other resources.
   * If you return an instance of this entry point in this method, it will be linked within the resource located at the
   * /content.rhyme path.
   * @param adapter for the incoming request that can be used to create and initialize the sling model implementation of
   *          your entry point resource
   * @return an optional providing the instance of the entry point resource (or empty if your API shouldn't be
   *         discoverable)
   */
  Optional<? extends LinkableResource> getApiEntryPoint(SlingResourceAdapter adapter);
}
