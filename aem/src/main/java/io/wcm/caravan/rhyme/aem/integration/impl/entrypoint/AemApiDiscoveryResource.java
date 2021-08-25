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
package io.wcm.caravan.rhyme.aem.integration.impl.entrypoint;

import java.util.List;

import io.wcm.caravan.rhyme.aem.integration.RhymeResourceRegistration;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * This resource is available on all systems where the Rhyme AEM integration bundle is installed. It's purpose
 * is that you have a single URL from which you can discover the entry points of all other services
 * implemented with caravan Rhyme that have been installed on this system. To achieve this,
 * the service developer has to implement
 * {@link RhymeResourceRegistration#getApiEntryPoint(SlingResourceAdapter)} to let the integration framework
 * know which resource from the API is the entry point that allows to access / discover all other resources.
 */
@HalApiInterface
public interface AemApiDiscoveryResource {

  /**
   * A list of links to the entry point of every Rhyme HAL API running on this server.
   * @return a list of {@link LinkableResource} used to create the link
   */
  @Related("hal:api")
  List<LinkableResource> getApiEntryPoints();
}
