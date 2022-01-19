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
package io.wcm.caravan.rhyme.aem.api.resources;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.sling.models.annotations.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.testing.context.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class AbstractLinkableResourceTest {

  private static final String DEFAULT_LINK_TITLE = "foo!";

  private AemContext context = AppAemContext.newAemContext();

  private SlingRhyme createRhymeInstance(String resourcePath) {

    return AppAemContext.createRhymeInstance(context, resourcePath);
  }

  @Test
  void adaptResource_should_get_all_RhymeObjects_injected() {

    SlingRhyme rhyme = createRhymeInstance("/foo");

    ResourceImpl resource = rhyme.adaptResource(rhyme.getRequestedResource(), ResourceImpl.class);

    assertThat(resource).isNotNull();
    assertThat(resource).hasNoNullFieldsOrPropertiesExcept("contextLinkTitle", "linkName");
  }

  @Test
  void createLink_should_use_default_link_title() {

    SlingRhyme rhyme = createRhymeInstance("/foo");

    ResourceImpl resource = rhyme.adaptResource(rhyme.getRequestedResource(), ResourceImpl.class);

    assertThat(resource.createLink().getTitle()).isEqualTo(DEFAULT_LINK_TITLE);
  }

  @Test
  void createLink_should_use_title_specified_with_setLinkTitle() {

    SlingRhyme rhyme = createRhymeInstance("/foo");

    ResourceImpl resource = rhyme.adaptResource(rhyme.getRequestedResource(), ResourceImpl.class);

    resource.getLinkProperties().setTitle("Custom Title");

    assertThat(resource.createLink().getTitle()).isEqualTo("Custom Title");
  }

  @Test
  void createLink_should_use_default_link_name_from_resource() {

    SlingRhyme rhyme = createRhymeInstance("/foo");

    ResourceImpl resource = rhyme.adaptResource(rhyme.getRequestedResource(), ResourceImpl.class);

    assertThat(resource.createLink().getName()).isEqualTo("foo");
  }

  @Test
  void createLink_should_use_link_name_specified_with_setLinkName() {

    SlingRhyme rhyme = createRhymeInstance("/foo");

    ResourceImpl resource = rhyme.adaptResource(rhyme.getRequestedResource(), ResourceImpl.class);

    resource.getLinkProperties().setName("bar");

    assertThat(resource.createLink().getName()).isEqualTo("bar");
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ResourceImpl extends AbstractLinkableResource {

    @Override
    protected String getDefaultLinkTitle() {
      return DEFAULT_LINK_TITLE;
    }
  }
}
