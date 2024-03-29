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
package io.wcm.caravan.rhyme.aem.testing.models;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.rhyme.aem.api.RhymeResourceRegistration;
import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Component
public class TestResourceRegistration implements RhymeResourceRegistration {

  @Override
  public Map<Class<? extends LinkableResource>, String> getModelClassesWithSelectors() {
    return ImmutableMap.of(SelectorSlingTestResource.class, SelectorSlingTestResource.SELECTOR);
  }

  @Override
  public Optional<? extends LinkableResource> getApiEntryPoint(SlingResourceAdapter adapter) {

    return adapter.selectResourceAt("/")
        .adaptTo(SelectorSlingTestResource.class)
        .getOptional();
  }
}
