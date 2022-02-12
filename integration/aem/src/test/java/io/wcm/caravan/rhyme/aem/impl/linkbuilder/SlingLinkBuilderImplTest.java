package io.wcm.caravan.rhyme.aem.impl.linkbuilder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.apache.sling.models.annotations.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.RhymeResourceRegistration;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.api.parameters.QueryParam;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.testing.context.AppAemContext;
import io.wcm.caravan.rhyme.aem.testing.models.SelectorSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.TestResourceRegistration;
import io.wcm.caravan.rhyme.aem.testing.models.UnregisteredSlingTestResource;
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
  void can_be_adapted_from_SlingRhyme() {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    assertThat(linkBuilder).isNotNull();
  }

  @Test
  void createLinkToCurrentResource_uses_registered_selector() {

    SlingRhyme slingRhyme = createRhymeInstance("/content");

    SlingLinkBuilder linkBuilder = slingRhyme.adaptTo(SlingLinkBuilder.class);

    SelectorSlingTestResource resource = slingRhyme.adaptResource(context.currentResource(), SelectorSlingTestResource.class);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getHref()).isEqualTo("/content.selectortest.rhyme");
  }

  @Test
  void createLinkToCurrentResource_handles_unregistered_resources() {

    SlingRhyme slingRhyme = createRhymeInstance("/content");

    SlingLinkBuilder linkBuilder = slingRhyme.adaptTo(SlingLinkBuilder.class);

    UnregisteredSlingTestResource resource = slingRhyme.adaptResource(context.currentResource(), UnregisteredSlingTestResource.class);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getHref()).isEqualTo("/content.rhyme");
  }

  private Link createLinkWithQueryParams(Consumer<ResourceWithParameters> paramProvider) {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    ResourceWithParameters resource = new ResourceWithParameters();
    paramProvider.accept(resource);

    return linkBuilder.createLinkToCurrentResource(resource);
  }

  @Test
  void createLinkToCurrentResource_includes_query_parameters() {

    Link link = createLinkWithQueryParams(resource -> {
      resource.foo = 1;
      resource.bar = 2;
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme?foo=1&bar=2");
  }

  @Test
  void createLinkToCurrentResource_strips_parameters_with_null_value() {


    Link link = createLinkWithQueryParams(resource -> {
      resource.bar = 1;
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme?bar=1");
  }

  @Test
  void createLinkToCurrentResource_encodes_characters() {

    Link link = createLinkWithQueryParams(resource -> {
      resource.string = "?/";
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme?string=%3F%2F");
  }

  @Test
  void createLinkToCurrentResource_strips_parameters_with_null_values() {

    Link link = createLinkWithQueryParams(queryParams -> {
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme");
  }

  private Link createLinkTemplateWithQueryParams(Consumer<ResourceWithParameters> paramProvider) {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    ResourceWithParameters resource = new ResourceWithParameters();

    paramProvider.accept(resource);
    resource.getLinkProperties().setTemplated(true);

    return linkBuilder.createLinkToCurrentResource(resource);
  }

  @Test
  void createLinkToCurrentResource_includes_query_parameters_fully_resolves_template() {

    Link link = createLinkTemplateWithQueryParams(resource -> {
      resource.foo = 1;
      resource.bar = 2;
      resource.string = "test";
    });


    assertThat(link.getHref()).isEqualTo("/content.rhyme?foo=1&bar=2&string=test");
  }

  @Test
  void createLinkToCurrentResource_keeps_template_variable_with_null_value() {

    Link link = createLinkTemplateWithQueryParams(resource -> {
      resource.bar = 1;
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme{?foo}&bar=1{&string}");
  }

  @Test
  void createLinkToCurrentResource_keeps_template_variables_with_null_values() {

    Link link = createLinkTemplateWithQueryParams(resource -> {

    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme{?foo,bar,string}");
  }

  @Test
  void createLinkToCurrentResource_uses_link_title() {

    String linkTitle = "This is the link title of the resource";

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new ResourceWithParameters();
    resource.getLinkProperties().setTitle(linkTitle);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getTitle()).isEqualTo(linkTitle);
  }

  @Test
  void createLinkToCurrentResource_uses_link_name() {

    String linkName = "foo";

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new ResourceWithParameters();
    resource.getLinkProperties().setName(linkName);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getName()).isEqualTo(linkName);
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ResourceWithParameters extends AbstractLinkableResource {

    @QueryParam
    private Integer foo;

    @QueryParam
    private Integer bar;

    @QueryParam
    private String string;

    @Override
    protected String getDefaultLinkTitle() {
      return "the default title";
    }

    public void setFoo(Integer foo) {
      this.foo = foo;
    }

    public void setBar(Integer bar) {
      this.bar = bar;
    }

    public void setString(String string) {
      this.string = string;
    }
  }

}
