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

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import io.wcm.caravan.rhyme.aem.integration.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.impl.ResourceSelectorRegistry;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class,
    adapters = { LinkableResource.class, AemApiDiscoveryResource.class },
    resourceType = "sling:OrderedFolder")
public class AemApiDiscoveryResourceImpl extends AbstractLinkableResource implements AemApiDiscoveryResource {

  public static final Integer MAX_AGE_SECONDS = 60;

  @Inject
  private ResourceSelectorRegistry registry;

  @Override
  public List<LinkableResource> getApiEntryPoints() {

    rhyme.setResponseMaxAge(Duration.ofSeconds(MAX_AGE_SECONDS));

    return registry.getAllApiEntryPoints(resourceAdapter)
        .collect(Collectors.toList());
  }

  @Override
  protected String getDefaultLinkTitle() {
    return "A list of all HAL+JSON APIs registered on this AEM instance";
  }

}
