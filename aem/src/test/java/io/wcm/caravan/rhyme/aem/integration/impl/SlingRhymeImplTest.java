package io.wcm.caravan.rhyme.aem.integration.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.aem.testing.api.SlingTestResource;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
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
  public void adaptResource_should_inject_SlingHttpServletRequest_instance() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    ModelWithSlingRequestField model = rhyme.adaptResource(context.currentResource(), ModelWithSlingRequestField.class);

    assertThat(model).isNotNull();
    assertThat(model.request).isNotNull();
  }

  @Model(adaptables = Resource.class)
  public static class ModelWithSlingRequestField {

    @RhymeObject
    private SlingHttpServletRequest request;
  }

  @Test
  public void adaptResource_should_inject_instance_adaptable_from_SlingHttpServletRequest() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    ModelWithFieldThatIsAdaptableFromRequest model = rhyme.adaptResource(context.currentResource(), ModelWithFieldThatIsAdaptableFromRequest.class);

    assertThat(model).isNotNull();
    assertThat(model.field).isNotNull();
    assertThat(model.field.request).isNotNull();
  }

  @Model(adaptables = SlingHttpServletRequest.class)
  public static class ModelThatIsAdaptableFromRequest {

    @Self
    private SlingHttpServletRequest request;
  }

  @Model(adaptables = Resource.class)
  public static class ModelWithFieldThatIsAdaptableFromRequest {

    @RhymeObject
    private ModelThatIsAdaptableFromRequest field;
  }

  @Test
  public void adaptResource_should_fail_to_inject_field_that_isnt_adaptable() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    Throwable ex = catchThrowable(() -> rhyme.adaptResource(context.currentResource(), ModelWithInvalidField.class));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to adapt")
        .hasCauseInstanceOf(HalApiDeveloperException.class);

    assertThat(ex.getCause())
        .hasMessageStartingWith("Cannot inject servlet field");
  }

  @Model(adaptables = Resource.class)
  public static class ModelWithInvalidField {

    @RhymeObject
    private HalApiServlet servlet;
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
  public void adaptResource_should_fail_if_class_is_not_a_sling_model() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    Throwable ex = catchThrowable(() -> rhyme.adaptResource(context.currentResource(), NotAnAdaptableClass.class));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to adapt")
        .hasMessageEndingWith(" to " + NotAnAdaptableClass.class.getSimpleName());
  }

  static class NotAnAdaptableClass {

  }

  @Test
  public void adaptResource_should_fail_if_model_is_not_adaptable_from_request() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    Throwable ex = catchThrowable(() -> rhyme.adaptResource(context.currentResource(), ModelAdaptablesFromRequest.class));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageEndingWith("is not declared to be adaptable from " + Resource.class);
  }

  @Model(adaptables = SlingHttpServletRequest.class)
  static class ModelAdaptablesFromRequest {

  }

  @Test
  public void getRemoteResource_should_fail_if_no_JsonResourceLoader_has_been_registered() {

    SlingRhyme rhyme = createRhymeInstance();

    // obtaining the client proxy will still work
    SlingTestResource resource = rhyme.getRemoteResource("http://localhost/foo", SlingTestResource.class);

    // but as soon as a method that triggers an HTTP request is called, things will fall apart
    Throwable ex = catchThrowable(() -> resource.getState());

    assertThat(ex).isInstanceOf(HalApiClientException.class)
        .hasMessageStartingWith("Failed to load an upstream resource that was requested by calling SlingTestResource#getState()")
        .hasCauseInstanceOf(HalApiClientException.class);

    assertThat(ex.getCause())
        .hasMessageStartingWith("No OSGi services implementing " + HalResourceLoader.class + " are running");
  }

  @Test
  public void getRemoteResource_should_use_registered_JsonResourceLoader_instance() {

    HalApiClientException clientException = new HalApiClientException("Simulated client failure", 403, null, null);

    HalResourceLoader resourceLoader = mock(HalResourceLoader.class);
    Mockito.when(resourceLoader.getHalResource(anyString()))
        .thenReturn(Single.error(clientException));
    context.registerService(HalResourceLoader.class, resourceLoader);

    SlingRhyme rhyme = createRhymeInstance();

    SlingTestResource resource = rhyme.getRemoteResource("http://localhost/foo", SlingTestResource.class);

    Throwable ex = catchThrowable(() -> resource.getState());

    assertThat(ex).isInstanceOf(HalApiClientException.class)
        .hasCause(clientException);
  }
}
