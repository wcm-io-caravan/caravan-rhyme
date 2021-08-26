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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.testing.api.SlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.api.SlingTestState;

@Model(adaptables = Resource.class, adapters = SlingTestResource.class)
public class SelectorSlingTestResource extends AbstractLinkableResource implements SlingTestResource {

  public static final String SELECTOR = "selectortest";

  public static final String DEFAULT_TITLE = "the default title of the test resource that is registered via selector";

  @Self
  private Resource resource;

  @Override
  public SlingTestState getState() {

    return new SlingTestState() {

      @Override
      public String getResourcePath() {
        return resource.getPath();
      }
    };
  }

  @Override
  protected String getDefaultLinkTitle() {
    return DEFAULT_TITLE;
  }

  @Override
  public String toString() {

    return getClass().getSimpleName() + " at " + resource.getPath();
  }

  @Override
  public SlingTestResource getSelfLinkWithPrefix() {
    return this;
  }

}
