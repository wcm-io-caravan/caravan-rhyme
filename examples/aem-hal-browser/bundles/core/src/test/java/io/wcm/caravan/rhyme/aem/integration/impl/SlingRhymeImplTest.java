package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.apache.http.HttpStatus;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
public class SlingRhymeImplTest {

  private AemContext context = AppAemContext.newAemContext();

  @Test
  public void can_be_adapted_from_SlingHttpServletRequest_to_impl_class() throws Exception {

    SlingRhymeImpl slingRhyme = context.request().adaptTo(SlingRhymeImpl.class);

    assertThat(slingRhyme).isNotNull();
  }

  @Test
  public void can_be_adapted_from_SlingHttpServletRequest_to_interface() throws Exception {

    SlingRhyme slingRhyme = context.request().adaptTo(SlingRhyme.class);

    assertThat(slingRhyme).isNotNull();
  }

  private SlingRhymeImpl createRhymeInstance() {

    Resource content = context.create().resource("/content");

    context.currentResource(content);
    context.request().setResource(content);

    return context.request().adaptTo(SlingRhymeImpl.class);
  }

  @Test
  public void getCurrentResource_should_initially_return_the_requested_resource() throws Exception {

    SlingRhyme slingRhyme = createRhymeInstance();

    assertThat(slingRhyme.getCurrentResource()).isSameAs(context.request().getResource());
  }

  @Test
  public void getRequestedResource_should_return_the_requested_resource() throws Exception {

    SlingRhyme slingRhyme = createRhymeInstance();

    assertThat(slingRhyme.getRequestedResource()).isSameAs(context.request().getResource());
  }

  @Test
  public void adaptResource_should_inject_SlingRhyme_instance() throws Exception {

    SlingRhymeImpl slingRhyme = createRhymeInstance();

    Resource resourceToAdapt = context.create().resource("/foo/bar");
    ModelWithSlingRhymeField model = slingRhyme.adaptResource(resourceToAdapt, ModelWithSlingRhymeField.class);

    assertThat(model).isNotNull();
    assertThat(model.slingRhyme).isInstanceOf(SlingRhymeImpl.class);

    // the injected SlingRhyme instance is a different instance (because the current resource might be different)
    assertThat(model.slingRhyme).isNotSameAs(slingRhyme);
    // but they both should share the same caravan Rhyme instance
    assertThat(((SlingRhymeImpl)model.slingRhyme).getCaravanRhyme()).isSameAs(slingRhyme.getCaravanRhyme());

    // the current resource is set to the resource from which the new model was adapted
    assertThat(model.slingRhyme.getCurrentResource()).isSameAs(resourceToAdapt);
    // but the requested resource is still set to the original resource from the request
    assertThat(model.slingRhyme.getRequestedResource()).isSameAs(context.request().getResource());
  }

  @Model(adaptables = Resource.class)
  public static class ModelWithSlingRhymeField {

    @RhymeObject
    private SlingRhyme slingRhyme;
  }

  @Test
  public void adaptResource_should_inject_SlingLinkBuilder_instance() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    ModelWithSlingLinkBuilderField model = rhyme.adaptResource(context.currentResource(), ModelWithSlingLinkBuilderField.class);

    assertThat(model).isNotNull();
    assertThat(model.linkBuilder).isNotNull();
  }

  @Model(adaptables = Resource.class)
  public static class ModelWithSlingLinkBuilderField {

    @RhymeObject
    private SlingLinkBuilder linkBuilder;
  }

  @Test
  public void adaptResource_should_fail_for_null_resource() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    Throwable ex = catchThrowable(() -> rhyme.adaptResource(null, ModelWithSlingRhymeField.class));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Cannot adapt null resource")
        .hasMessageEndingWith(" to " + ModelWithSlingRhymeField.class.getSimpleName());
  }

  @Test
  public void adaptResource_should_fail_if_adaption_not_possible() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    Throwable ex = catchThrowable(() -> rhyme.adaptResource(context.currentResource(), NotAnAdaptableClass.class));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to adapt")
        .hasMessageEndingWith(" to " + NotAnAdaptableClass.class.getSimpleName());
  }

  static class NotAnAdaptableClass {

  }

  @Test
  public void renderRequestedResource() throws Exception {

    SlingRhymeImpl rhyme = createRhymeInstance();

    HalResponse response = rhyme.renderRequestedResource();

    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    assertThat(response.getBody().getLink().getHref()).isEqualTo(TestLinkableResource.TEST_PATH);
  }

  @HalApiInterface
  public interface TestResource extends LinkableResource {

  }

  @Model(adaptables = Resource.class, adapters = LinkableResource.class, resourceType = "nt:unstructured")
  public static class TestLinkableResource implements TestResource {

    private static final String TEST_PATH = "/foo/bar.json";

    @Override
    public Link createLink() {
      return new Link(TEST_PATH);
    }

  }
}
