package io.wcm.caravan.rhyme.aem.impl.linkbuilder;

import static io.wcm.caravan.rhyme.aem.impl.linkbuilder.UrlFingerprintingImpl.TIMESTAMP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.function.Consumer;

import org.apache.sling.models.annotations.Model;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.RhymeResourceRegistration;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.FingerprintBuilder;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.api.parameters.QueryParam;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.api.resources.ImmutableResource;
import io.wcm.caravan.rhyme.aem.testing.context.AppAemContext;
import io.wcm.caravan.rhyme.aem.testing.models.SelectorSlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.models.TestResourceRegistration;
import io.wcm.caravan.rhyme.aem.testing.models.UnregisteredSlingTestResource;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
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

  private SlingLinkBuilderImpl createLinkBuilder(String resourcePath) {

    SlingRhyme slingRhyme = createRhymeInstance(resourcePath);

    return slingRhyme.adaptTo(SlingLinkBuilderImpl.class);
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

  private Link createLinkWithQueryParams(Consumer<ResourceWithParameters> paramProvider) {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    ResourceWithParameters resource = new ResourceWithParameters();
    paramProvider.accept(resource);

    return linkBuilder.createLinkToCurrentResource(resource);
  }

  @Test
  public void createLinkToCurrentResource_includes_query_parameters() throws Exception {

    Link link = createLinkWithQueryParams(resource -> {
      resource.foo = 1;
      resource.bar = 2;
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme?foo=1&bar=2");
  }

  @Test
  public void createLinkToCurrentResource_strips_parameters_with_null_value() throws Exception {


    Link link = createLinkWithQueryParams(resource -> {
      resource.bar = 1;
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme?bar=1");
  }

  @Test
  public void createLinkToCurrentResource_encodes_characters() throws Exception {

    Link link = createLinkWithQueryParams(resource -> {
      resource.string = "?/";
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme?string=%3F%2F");
  }

  @Test
  public void createLinkToCurrentResource_strips_parameters_with_null_values() throws Exception {

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
  public void createLinkToCurrentResource_includes_query_parameters_fully_resolves_template() throws Exception {

    Link link = createLinkTemplateWithQueryParams(resource -> {
      resource.foo = 1;
      resource.bar = 2;
      resource.string = "test";
    });


    assertThat(link.getHref()).isEqualTo("/content.rhyme?foo=1&bar=2&string=test");
  }

  @Test
  public void createLinkToCurrentResource_keeps_template_variable_with_null_value() throws Exception {

    Link link = createLinkTemplateWithQueryParams(resource -> {
      resource.bar = 1;
    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme{?foo}&bar=1{&string}");
  }

  @Test
  public void createLinkToCurrentResource_keeps_template_variables_with_null_values() throws Exception {

    Link link = createLinkTemplateWithQueryParams(resource -> {

    });

    assertThat(link.getHref()).isEqualTo("/content.rhyme{?foo,bar,string}");
  }

  @Test
  public void createLinkToCurrentResource_uses_link_title() throws Exception {

    String linkTitle = "This is the link title of the resource";

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new ResourceWithParameters();
    resource.getLinkProperties().setTitle(linkTitle);

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getTitle()).isEqualTo(linkTitle);
  }

  @Test
  public void createLinkToCurrentResource_uses_link_name() throws Exception {

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

  @Test
  public void createLinkToCurrentResource_can_append_timestamp_from_current_request() throws Exception {

    String incomingQuery = TIMESTAMP + "=foo";
    context.request().setQueryString(incomingQuery);

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new ResourceWithTimestampFromRequest();

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getHref())
        .isEqualTo("/content.rhyme?" + incomingQuery);
  }

  @Test
  public void createLinkToCurrentResource_does_not_expect_timestamp_to_be_present_in_current_request() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new ResourceWithTimestampFromRequest();

    Link link = linkBuilder.createLinkToCurrentResource(resource);

    assertThat(link.getHref())
        .isEqualTo("/content.rhyme");
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ResourceWithTimestampFromRequest extends AbstractLinkableResource implements ImmutableResource {

    @Override
    public void buildFingerprint(FingerprintBuilder fingerprint) {

      fingerprint.useFingerprintFromIncomingRequest();
    }

    @Override
    protected String getDefaultLinkTitle() {
      return "default link title";
    }
  }

  @Test
  public void createLinkToCurrentResource_fails_if_no_fingerprint_was_built() throws Exception {

    SlingLinkBuilder linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = new ResourceWithIncompleteFingerprint();

    Throwable ex = catchThrowable(() -> linkBuilder.createLinkToCurrentResource(resource));

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageContaining("must call at least one of the methods from the builder");
  }


  @Model(adaptables = SlingRhyme.class)
  public static class ResourceWithIncompleteFingerprint extends AbstractLinkableResource implements ImmutableResource {

    @Override
    public void buildFingerprint(FingerprintBuilder fingerprint) {

      // we are not calling any method on the builder on purpose
    }

    @Override
    protected String getDefaultLinkTitle() {
      return "default link title";
    }
  }

  @Test
  public void createLinkToCurrentResource_fails_if_resource_is_not_a_JcrNode() throws Exception {

    SlingLinkBuilderImpl linkBuilder = createLinkBuilder("/content");

    AbstractLinkableResource resource = linkBuilder.getSlingRhyme()
        .adaptTo(UrlFingerprintingImplTest.ResourceWithLastModifiedBelowContentTimestamp.class);

    // this fails because unit-tests from this class are not using a AemContext using ResourceResolverType.JCR_MOCK
    // the same test would work when executde UrlFingerPrintingImplTest which does use mocked JCR resources
    Throwable ex = catchThrowable(() -> linkBuilder.createLinkToCurrentResource(resource));

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessage("Failed to get most recent cq:lastModified below /content");

    assertThat(ex)
        .hasRootCauseMessage("Could not adapt ResourceResolver to JCR Session");
  }

}
