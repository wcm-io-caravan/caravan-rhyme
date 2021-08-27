package io.wcm.caravan.rhyme.aem.impl.linkbuilder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.RhymeResourceRegistration;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.testing.models.SelectorSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.TestResourceRegistration;
import io.wcm.caravan.rhyme.aem.testing.models.UnregisteredSlingTestResource;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
public class SlingLinkBuilderImplTest {

  private AemContext context = AppAemContext.newAemContext();

  @BeforeEach
  void setUp() {
    context.registerService(RhymeResourceRegistration.class, new TestResourceRegistration());
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

    SlingRhyme slingRhyme = createRhymeInstance("/content");

    SlingLinkBuilder linkBuilder = slingRhyme.adaptTo(SlingLinkBuilder.class);

    SelectorSlingTestResource resource = slingRhyme.adaptResource(context.currentResource(), SelectorSlingTestResource.class);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getHref()).isEqualTo("/content.selectortest.rhyme");
  }

  @Test
  public void createLinkToCurrentResource_handles_unregistered_resources() throws Exception {

    SlingRhyme slingRhyme = createRhymeInstance("/content");

    SlingLinkBuilder linkBuilder = slingRhyme.adaptTo(SlingLinkBuilder.class);

    UnregisteredSlingTestResource resource = slingRhyme.adaptResource(context.currentResource(), UnregisteredSlingTestResource.class);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getHref()).isEqualTo("/content.rhyme");
  }

  private Link createLinkWithQueryParams(Consumer<Map<String, Object>> paramProvider) {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new LinkableResourceImpl();
    paramProvider.accept(resource.getLinkProperties().getQueryParameters());

    return linkBuilder.createLinkToCurrentResource(resource);
  }

  @Test
  public void createLinkToCurrentResource_includes_query_parameters() throws Exception {

    Link link = createLinkWithQueryParams(queryParams -> {
      queryParams.put("foo", 1);
      queryParams.put("bar", 2);
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme?foo=1&bar=2");
  }

  @Test
  public void createLinkToCurrentResource_strips_parameters_with_null_value() throws Exception {


    Link link = createLinkWithQueryParams(queryParams -> {
      queryParams.put("bar", 1);
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme?bar=1");
  }

  @Test
  public void createLinkToCurrentResource_strips_parameters_with_null_values() throws Exception {

    Link link = createLinkWithQueryParams(queryParams -> {
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme");
  }

  private Link createLinkTemplateWithQueryParams(Consumer<Map<String, Object>> paramProvider) {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new LinkableResourceImpl();

    paramProvider.accept(resource.getLinkProperties().getQueryParameters());
    resource.getLinkProperties().setTemplated(true);

    return linkBuilder.createLinkToCurrentResource(resource);
  }

  @Test
  public void createLinkToCurrentResource_includes_query_parameters_fully_resolves_template() throws Exception {

    Link link = createLinkTemplateWithQueryParams(queryParams -> {
      queryParams.put("foo", 1);
      queryParams.put("bar", 2);
    });


    assertThat(link.getHref()).isEqualTo("/content.rhyme?foo=1&bar=2");
  }

  @Test
  public void createLinkToCurrentResource_keeps_template_variable_with_null_value() throws Exception {

    Link link = createLinkTemplateWithQueryParams(queryParams -> {
      queryParams.put("foo", null);
      queryParams.put("bar", 1);
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme{?foo}&bar=1");
  }

  @Test
  public void createLinkToCurrentResource_keeps_template_variables_with_null_values() throws Exception {

    Link link = createLinkTemplateWithQueryParams(queryParams -> {
      queryParams.put("foo", null);
      queryParams.put("bar", null);
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme{?foo,bar}");
  }

  @Test
  public void createLinkToCurrentResource_uses_link_title() throws Exception {

    String linkTitle = "This is the link title of the resource";

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new LinkableResourceImpl();
    resource.getLinkProperties().setTitle(linkTitle);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getTitle()).isEqualTo(linkTitle);
  }

  @Test
  public void createLinkToCurrentResource_uses_link_name() throws Exception {

    String linkName = "foo";

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new LinkableResourceImpl();
    resource.getLinkProperties().setName(linkName);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getName()).isEqualTo(linkName);
  }

  public static class LinkableResourceImpl extends AbstractLinkableResource {

    @Override
    protected String getDefaultLinkTitle() {
      return "the default title";
    }

  }

}
