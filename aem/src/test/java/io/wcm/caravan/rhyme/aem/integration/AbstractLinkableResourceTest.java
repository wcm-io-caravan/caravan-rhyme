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
package io.wcm.caravan.rhyme.aem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
public class AbstractLinkableResourceTest {

  private static final String FOO = "foo!";

  private AemContext context = AppAemContext.newAemContext();

  private SlingRhyme createRhymeInstance(String resourcePath) {

    return AppAemContext.createRhymeInstance(context, resourcePath);
  }

  @Test
  public void should_get_all_RhymeObjects_injected() {

    SlingRhyme rhyme = createRhymeInstance("/foo");

    ResourceImpl impl = rhyme.adaptResource(rhyme.getRequestedResource(), ResourceImpl.class);

    assertThat(impl).isNotNull();
    assertThat(impl).hasNoNullFieldsOrPropertiesExcept("contextLinkTitle");
  }

  @Model(adaptables = Resource.class)
  public static class ResourceImpl extends AbstractLinkableResource {

    @Override
    protected String getDefaultLinkTitle() {
      return FOO;
    }
  }
}
