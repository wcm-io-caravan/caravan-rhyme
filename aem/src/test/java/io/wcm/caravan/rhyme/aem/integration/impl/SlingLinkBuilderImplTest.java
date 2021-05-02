package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.integration.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.ResourceSelectorProvider;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.aem.testing.api.SlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.ResourceTypeSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.SelectorSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.TestResourceSelectorProvider;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
public class SlingLinkBuilderImplTest {

  private AemContext context = AppAemContext.newAemContext();

  @BeforeEach
  void setUp() {
    context.registerService(ResourceSelectorProvider.class, new TestResourceSelectorProvider());
  }

  private SlingRhyme createRhymeInstance(String resourcePath) {

    return AppAemContext.createRhymeInstance(context, resourcePath);
  }

  private SlingLinkBuilder createLinkBuilder(String resourcePath) {

    SlingRhyme slingRhyme = createRhymeInstance(resourcePath);

    return slingRhyme.adaptTo(SlingLinkBuilder.class);
  }

  @Test
  public void can_be_adapted_from_SlingRhyme() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    assertThat(linkBuilder).isNotNull();
  }

  @Test
  public void createLinkToCurrentResource_uses_registered_selector() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    SelectorSlingTestResource resource = context.currentResource().adaptTo(SelectorSlingTestResource.class);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getHref()).isEqualTo("/content.selectortest.rhyme");
  }

  @Test
  public void createLinkToCurrentResource_handles_unregistered_resources() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    ResourceTypeSlingTestResource resource = context.currentResource().adaptTo(ResourceTypeSlingTestResource.class);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getHref()).isEqualTo("/content.rhyme");
  }

  @Test
  public void createLinkToCurrentResource_includes_query_parameters() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new AbstractLinkableResource() {

      @Override
      public Map<String, Object> getQueryParameters() {
        return ImmutableMap.of("foo", "1", "bar", "2");
      }

      @Override
      protected String getDefaultLinkTitle() {
        return "the default title";
      }

    };

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getHref()).isEqualTo("/content.rhyme?foo=1&bar=2");
  }

  @Test
  public void createLinkToCurrentResource_strips_parameters_with_null_value() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new AbstractLinkableResource() {

      @Override
      public Map<String, Object> getQueryParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("foo", null);
        params.put("bar", "1");
        return params;
      }

      @Override
      protected String getDefaultLinkTitle() {
        return "the default title";
      }

    };

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getHref()).isEqualTo("/content.rhyme?bar=1");
  }

  @Test
  public void createLinkToCurrentResource_uses_link_title() throws Exception {

    String linkTitle = "This is the link title of the resource";

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    SlingLinkableResource resource = mock(SlingLinkableResource.class);
    when(resource.getLinkTitle()).thenReturn(linkTitle);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getTitle()).isEqualTo(linkTitle);
  }

  @Test
  public void buildTemplateTo_uses_selector_for_registered_class() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    SlingTestResource resource = linkBuilder.buildTemplateTo(SlingTestResource.class)
        .buildRequired();

    assertThat(resource.createLink().getHref()).isEqualTo("{+path}.selectortest.rhyme");
  }

  @Test
  public void buildTemplateTo_handles_unregistered_resources() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    UnregisteredResource resource = linkBuilder.buildTemplateTo(UnregisteredResource.class)
        .buildRequired();

    assertThat(resource.createLink().getHref()).isEqualTo("{+path}.rhyme");
  }

  @HalApiInterface
  interface UnregisteredResource extends LinkableResource {

  }

  @Test
  public void buildTemplateTo_uses_link_title() throws Exception {

    String linkTitle = "This is the link title for the template";

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    UnregisteredResource resource = linkBuilder.buildTemplateTo(UnregisteredResource.class)
        .withTitle(linkTitle)
        .buildRequired();

    assertThat(resource.createLink().getTitle()).isEqualTo(linkTitle);
  }

  @Test
  public void buildTemplateTo_appends_query_parameters_title() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    UnregisteredResource resource = linkBuilder.buildTemplateTo(UnregisteredResource.class)
        .withQueryParameters("foo", "bar")
        .buildRequired();

    assertThat(resource.createLink().getHref()).isEqualTo("{+path}.rhyme{?foo,bar}");
  }

  @Test
  public void buildTemplateTo_can_return_optional() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    Optional<UnregisteredResource> resource = linkBuilder.buildTemplateTo(UnregisteredResource.class)
        .buildOptional();

    assertThat(resource).isPresent();
    assertThat(resource.get().createLink()).isNotNull();
  }

  @Test
  public void buildTemplateTo_fails_if_any_other_method_is_called_on_proxy() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    SlingTestResource resource = linkBuilder.buildTemplateTo(SlingTestResource.class)
        .buildRequired();

    Throwable ex = catchThrowable(() -> resource.getState());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Unsupported call to getState method");
  }
}
