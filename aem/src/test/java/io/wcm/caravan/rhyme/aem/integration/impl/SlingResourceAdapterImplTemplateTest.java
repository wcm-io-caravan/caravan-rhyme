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
package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.rhyme.aem.integration.ResourceSelectorProvider;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.aem.testing.api.SlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.TestResourceSelectorProvider;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
public class SlingResourceAdapterImplTemplateTest {

  private AemContext context = AppAemContext.newAemContext();

  @BeforeEach
  void setUp() {
    context.registerService(ResourceSelectorProvider.class, new TestResourceSelectorProvider());
  }

  private SlingResourceAdapterImpl createAdapterInstanceForResource(String resourcePath) {

    SlingRhyme rhyme = createRhymeInstance(resourcePath);

    return rhyme.adaptTo(SlingResourceAdapterImpl.class);
  }

  private SlingRhyme createRhymeInstance(String resourcePath) {

    return AppAemContext.createRhymeInstance(context, resourcePath);
  }

  @Test
  public void getOptional_can_be_called_on_ResourceAdapter() {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/");

    Optional<SlingTestResource> resource = adapter.selectResourceAt(null)
        .adaptTo(SlingTestResource.class)
        .getOptional();

    assertThat(resource.isPresent());
  }

  @Test
  public void getStream_can_be_called_on_ResourceAdapter() {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/");

    List<SlingTestResource> resources = adapter.selectResourceAt(null)
        .adaptTo(SlingTestResource.class)
        .getStream()
        .collect(Collectors.toList());

    assertThat(resources).hasSize(1);
  }

  @Test
  public void selectResourceAt_generates_templates_for_null_path() {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/");

    SlingTestResource resource = adapter.selectResourceAt(null)
        .adaptTo(SlingTestResource.class)
        .getInstance();

    assertThat(resource.createLink()).isNotNull();
    assertThat(resource.createLink().isTemplated()).isTrue();
    assertThat(resource.createLink().getHref()).isEqualTo("{+path}.selectortest.rhyme");
  }

  @Test
  public void selectResourceAt_uses_link_title_if_null_path_is_given() {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/");

    String linkTitle = "A title for the template";

    SlingTestResource resource = adapter.selectResourceAt(null)
        .adaptTo(SlingTestResource.class)
        .withLinkTitle(linkTitle)
        .getInstance();

    assertThat(resource.createLink().getTitle()).isEqualTo(linkTitle);
  }

  @Test
  public void selectResourceAt_appends_query_parameters_if_null_path_is_given() {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/");

    SlingTestResource resource = adapter.selectResourceAt(null)
        .adaptTo(SlingTestResource.class)
        .withQueryParameters("foo", "bar")
        .getInstance();

    assertThat(resource.createLink().getHref()).isEqualTo("{+path}.selectortest.rhyme{?foo,bar}");
  }


  @Test
  public void selectResourceAt_can_be_used_to_build_templates_for_unregistered_resources() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/");

    UnregisteredResource resource = adapter.selectResourceAt(null)
        .adaptTo(UnregisteredResource.class)
        .getInstance();

    assertThat(resource.createLink()).isNotNull();
    assertThat(resource.createLink().getHref()).isEqualTo("{+path}.rhyme");
  }

  @HalApiInterface
  interface UnregisteredResource extends LinkableResource {

  }

  @Test
  public void buildTemplateTo_fails_if_any_other_method_is_called_on_proxy() throws Exception {

    SlingResourceAdapterImpl adapter = createAdapterInstanceForResource("/");

    String linkTitle = "A title for the template";

    SlingTestResource resource = adapter.selectResourceAt(null)
        .adaptTo(SlingTestResource.class)
        .withLinkTitle(linkTitle)
        .getInstance();

    Throwable ex = catchThrowable(() -> resource.getState());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Unsupported call to getState method");
  }

}
