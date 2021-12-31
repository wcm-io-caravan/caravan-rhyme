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
package io.wcm.caravan.rhyme.aem.impl.adaptation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.apache.sling.models.annotations.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.RhymeResourceRegistration;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.api.resources.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.impl.linkbuilder.SlingLinkBuilderImplTest.ResourceWithParameters;
import io.wcm.caravan.rhyme.aem.testing.api.SlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.context.AppAemContext;
import io.wcm.caravan.rhyme.aem.testing.models.SelectorSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.TestResourceRegistration;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
public class SlingModelPostAdaptationStageTest {

  private AemContext context = AppAemContext.newAemContext();

  @BeforeEach
  void setUp() {
    context.registerService(RhymeResourceRegistration.class, new TestResourceRegistration());
  }

  private SlingResourceAdapterImpl createAdapterInstanceForResource(String resourcePath) {

    SlingRhyme rhyme = createRhymeInstance(resourcePath);

    return rhyme.adaptTo(SlingResourceAdapterImpl.class);
  }

  private SlingRhyme createRhymeInstance(String resourcePath) {

    return AppAemContext.createRhymeInstance(context, resourcePath);
  }


  @Test
  public void withLinkTitle_allows_to_overide_title_for_SlingLinkableResource_instances() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    String customTitle = "New Link Title";

    Link link = adapter.selectCurrentResource()
        .adaptTo(SlingTestResource.class)
        .withLinkTitle(customTitle)
        .getInstance()
        .createLink();

    assertThat(link.getTitle()).isEqualTo(customTitle);
  }

  @Test
  public void withLinkTitle_fails_to_overide_title_if_model_class_does_not_implement_SlingLinkableResource() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    String customTitle = "New Link Title";

    Throwable ex = catchThrowable(() -> adapter.selectCurrentResource()
        .adaptTo(ClassThatDoesNotImplementSlingLinkableResource.class)
        .withLinkTitle(customTitle)
        .getInstance());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Your model class " + ClassThatDoesNotImplementSlingLinkableResource.class.getSimpleName() + " does not implement "
            + SlingLinkableResource.class.getName());

  }

  @Test
  public void withLinkName_allows_to_overide_name_for_SlingLinkableResource_instances() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    String customName = "custom-name";

    Link link = adapter.selectCurrentResource()
        .adaptTo(SlingTestResource.class)
        .withLinkName(customName)
        .getInstance()
        .createLink();

    assertThat(link.getName()).isEqualTo(customName);
  }

  @Test
  public void withLinkName_and_withLinkTitle_can_be_combined() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    String customName = "custom-name";
    String customTitle = "Custom Title";

    Link link = adapter.selectCurrentResource()
        .adaptTo(SlingTestResource.class)
        .withLinkName(customName)
        .withLinkTitle(customTitle)
        .getInstance()
        .createLink();

    assertThat(link.getName()).isEqualTo(customName);
    assertThat(link.getTitle()).isEqualTo(customTitle);
  }


  @Model(adaptables = SlingRhyme.class)
  public static class ClassThatDoesNotImplementSlingLinkableResource implements EmbeddableResource {

  }

  @Test
  public void withModifications_should_allow_to_append_muliple_query_parameters() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    Link link = adapter.selectCurrentResource()
        .adaptTo(ResourceWithParameters.class)
        .withModifications(resource -> resource.setFoo(123))
        .withModifications(resource -> resource.setBar(456))
        .getInstance()
        .createLink();

    assertThat(link.getHref()).isEqualTo("/content.rhyme?foo=123&bar=456");
  }

  @Test
  public void withQueryParameterTemplate_fails_if_non_null_resource_path_was_selected() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");


    Throwable ex = catchThrowable(() -> adapter.selectCurrentResource()
        .adaptTo(ClassThatDoesNotImplementSlingLinkableResource.class)
        .withQueryParameterTemplate("foo")
        .getInstance());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("#withQueryParameterTemplatecan can only be called if you selected a null resource path to create a template");

  }

  @Test
  public void withPartialLinkTemplate_should_keep_parameters_with_null_values() {

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content");

    Link link = adapter.selectCurrentResource()
        .adaptTo(ResourceWithParameters.class)
        .withModifications(resource -> resource.setFoo(123))
        .withPartialLinkTemplate()
        .getInstance()
        .createLink();

    assertThat(link.getHref()).isEqualTo("/content.rhyme?foo=123{&bar,string}");
  }

  @Test
  public void withModifications_should_allow_calls_to_impl_instance() {

    String customTitle = "This is a link to foo!";

    SlingResourceAdapter adapter = createAdapterInstanceForResource("/content/foo");

    Link link = adapter.selectCurrentResource()
        .adaptTo(SlingTestResource.class, SelectorSlingTestResource.class)
        .withModifications(impl -> impl.getLinkProperties().setTitle(customTitle))
        .getInstance()
        .createLink();

    assertThat(link.getTitle())
        .isEqualTo(customTitle);
  }


}
