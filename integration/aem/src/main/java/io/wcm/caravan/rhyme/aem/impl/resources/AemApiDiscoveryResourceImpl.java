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
package io.wcm.caravan.rhyme.aem.impl.resources;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.sling.models.annotations.Model;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.impl.RhymeResourceRegistry;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = SlingRhyme.class, adapters = AemApiDiscoveryResource.class)
public class AemApiDiscoveryResourceImpl extends AbstractLinkableResource implements AemApiDiscoveryResource {

  public static final Integer MAX_AGE_SECONDS = 60;

  @Inject
  private RhymeResourceRegistry registry;

  @Override
  public List<Link> getApiEntryPoints() {

    rhyme.setResponseMaxAge(Duration.ofSeconds(MAX_AGE_SECONDS));

    return registry.getAllApiEntryPoints(resourceAdapter)
        .map(LinkableResource::createLink)
        .collect(Collectors.toList());
  }

  @Override
  protected String getDefaultLinkTitle() {
    return "A list of all HAL+JSON APIs registered on this AEM instance";
  }

}
