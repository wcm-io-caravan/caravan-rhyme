package io.wcm.caravan.rhyme.aem.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.UnknownHostException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.wcm.api.Page;
import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.testing.api.SlingTestResource;
import io.wcm.caravan.rhyme.aem.testing.context.AppAemContext;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
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

    assertThat(model.slingRhyme).isInstanceOf(SlingRhymeImpl.class);

    // the injected SlingRhyme instance is a different instance (because the current resource might be different)
    assertThat(model.slingRhyme).isNotSameAs(slingRhyme);
    // but they both should share the same caravan Rhyme instance
    assertThat(model.slingRhyme.getCoreRhyme()).isSameAs(slingRhyme.getCoreRhyme());

    // the current resource is set to the resource from which the new model was adapted
    assertThat(model.slingRhyme.getCurrentResource()).isSameAs(resourceToAdapt);
    // but the requested resource is still set to the original resource from the request
    assertThat(model.slingRhyme.getRequestedResource()).isSameAs(context.request().getResource());
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithSlingRhymeField {

    @Self
    private SlingRhyme slingRhyme;
  }

  @Test
  public void adaptResource_should_inject_SlingLinkBuilder_instance() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    ModelWithSlingLinkBuilderField model = rhyme.adaptResource(context.currentResource(), ModelWithSlingLinkBuilderField.class);

    assertThat(model.linkBuilder).isNotNull();
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithSlingLinkBuilderField {

    @Self
    private SlingLinkBuilder linkBuilder;
  }

  @Test
  public void adaptResource_should_inject_SlingHttpServletRequest_instance() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    ModelWithSlingRequestField model = rhyme.adaptResource(context.currentResource(), ModelWithSlingRequestField.class);

    assertThat(model.request).isNotNull();
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithSlingRequestField {

    @Self
    private SlingHttpServletRequest request;
  }

  @Test
  public void adaptResource_should_inject_instance_adaptable_from_SlingHttpServletRequest() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    ModelWithFieldThatIsAdaptableFromRequest model = rhyme.adaptResource(context.currentResource(), ModelWithFieldThatIsAdaptableFromRequest.class);

    assertThat(model.field).isNotNull();
    assertThat(model.field.request).isNotNull();
  }

  @Model(adaptables = SlingHttpServletRequest.class)
  public static class ModelThatIsAdaptableFromRequest {

    @Self
    private SlingHttpServletRequest request;
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithFieldThatIsAdaptableFromRequest {

    @Self
    private ModelThatIsAdaptableFromRequest field;
  }

  @Test
  public void adaptResource_should_inject_Resource_instance() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    ModelWithSlingResourceField model = rhyme.adaptResource(context.currentResource(), ModelWithSlingResourceField.class);

    assertThat(model.resource).isNotNull();
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithSlingResourceField {

    @Self
    private Resource resource;
  }

  @Test
  public void adaptResource_should_inject_instance_adaptable_from_Resource() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    ModelWithFieldThatIsAdaptableFromResource model = rhyme.adaptResource(context.currentResource(), ModelWithFieldThatIsAdaptableFromResource.class);

    assertThat(model.field).isNotNull();
    assertThat(model.field.resource).isNotNull();
  }

  interface InterfaceWithResource {

    Resource getResource();
  }

  @Model(adaptables = Resource.class, adapters = InterfaceWithResource.class)
  public static class ModelThatIsAdaptableFromResource implements InterfaceWithResource {

    @Self
    private Resource resource;

    @Override
    public Resource getResource() {
      return resource;
    }
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithFieldThatIsAdaptableFromResource {

    @Self
    private ModelThatIsAdaptableFromResource field;
  }

  @Test
  public void adaptResource_should_inject_interface_instance_adaptable_from_Resource() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    ModelWithFieldThatIsAnInterfaceOfAnAdaptableFromResource model = rhyme.adaptResource(context.currentResource(),
        ModelWithFieldThatIsAnInterfaceOfAnAdaptableFromResource.class);

    assertThat(model.field).isNotNull();
    assertThat(model.field.getResource()).isNotNull();
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithFieldThatIsAnInterfaceOfAnAdaptableFromResource {

    @Self
    private InterfaceWithResource field;
  }

  @Test
  public void adaptResource_should_inject_ValueMapValue_instance() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    String resourcePath = context.create().resource("/foo", ImmutableMap.of("bar", "abc")).getPath();

    ModelWithValueMapValueField model = rhyme.adaptResource(context.currentResource(resourcePath), ModelWithValueMapValueField.class);

    assertThat(model.bar).isEqualTo("abc");
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithValueMapValueField {

    @ValueMapValue
    private String bar;
  }

  @Test
  public void adaptResource_should_inject_Page_instance_if_page_path_was_requested() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    String pagePath = context.create().page("/foo").getPath();

    ModelWithPageField model = rhyme.adaptResource(context.currentResource(pagePath), ModelWithPageField.class);

    assertThat(model.page).isNotNull();
  }

  @Test
  public void adaptResource_should_inject_Page_instance_if_content_path_was_requested() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    String contentPath = context.create().page("/foo").getContentResource().getPath();

    ModelWithPageField model = rhyme.adaptResource(context.currentResource(contentPath), ModelWithPageField.class);

    assertThat(model.page).isNotNull();
    assertThat(model.page.getPath()).isEqualTo("/foo");
  }

  @Test
  public void adaptResource_should_inject_Page_instance_if_resource_below_content_path_was_requested() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    Resource content = context.create().page("/foo").getContentResource();
    String childPath = context.create().resource(content, "bar").getPath();

    ModelWithPageField model = rhyme.adaptResource(context.currentResource(childPath), ModelWithPageField.class);

    assertThat(model.page).isNotNull();
    assertThat(model.page.getPath()).isEqualTo("/foo");
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithPageField {

    @Self
    private Page page;
  }

  @Test
  public void adaptResource_should_fail_to_inject_field_that_isnt_adaptable() throws Exception {

    SlingRhyme rhyme = createRhymeInstance();

    Throwable ex = catchThrowable(() -> rhyme.adaptResource(context.currentResource(), ModelWithInvalidField.class));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Failed to adapt");

    assertThat(ex.getCause())
        .hasMessageStartingWith("Could not inject all required fields");
  }

  @Model(adaptables = SlingRhyme.class)
  public static class ModelWithInvalidField {

    @Self
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
        .hasMessageEndingWith("is not declared to be adaptable from " + SlingRhyme.class);
  }

  @Model(adaptables = SlingHttpServletRequest.class)
  static class ModelAdaptablesFromRequest {

  }

  @Test
  public void getRemoteResource_should_use_resource_loader_created_with_HttpClientFactory() {

    SlingRhyme rhyme = createRhymeInstance();

    // obtaining the client proxy will still work
    SlingTestResource resource = rhyme.getRemoteResource("http://foo.bar", SlingTestResource.class);

    // but as soon as a method that triggers an HTTP request is called, things will fall apart
    Throwable ex = catchThrowable(() -> resource.getState());

    assertThat(ex).isInstanceOf(HalApiClientException.class)
        .hasMessageStartingWith("Failed to load an upstream resource that was requested by calling SlingTestResource#getState()")
        .hasCauseInstanceOf(HalApiClientException.class)
        .hasRootCauseInstanceOf(UnknownHostException.class);

    assertThat(ex.getCause())
        .hasMessageStartingWith("HAL client request to http://foo.bar has failed");

  }
}
