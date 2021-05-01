package io.wcm.caravan.rhyme.aem.integration.impl;

import java.time.Duration;
import java.util.Arrays;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;

import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingHttpServletRequest.class, adapters = { SlingRhyme.class, SlingRhymeImpl.class })
public class SlingRhymeImpl extends SlingAdaptable implements SlingRhyme {

  private final SlingHttpServletRequest request;
  private final Resource currentResource;
  private final Rhyme rhyme;

  @Inject
  public SlingRhymeImpl(@Self SlingHttpServletRequest request) {

    this.request = request;

    this.currentResource = request.getResource();

    this.rhyme = RhymeBuilder.withoutResourceLoader()
        .buildForRequestTo(request.getRequestURL().toString());
  }

  SlingRhymeImpl(SlingRhymeImpl slingRhyme, Resource currentResource) {

    this.request = slingRhyme.request;
    this.currentResource = currentResource;
    this.rhyme = slingRhyme.rhyme;
  }

  @Override
  public <@NotNull T> T adaptResource(Resource resource, @NotNull Class<T> modelClass) {

    if (resource == null) {
      throw new HalApiDeveloperException("Cannot adapt null resource to " + modelClass.getSimpleName());
    }

    verifyModelAnnotationIfPresent(modelClass);

    try {
      T slingModel = resource.adaptTo(modelClass);
      if (slingModel == null) {
        throw new HalApiDeveloperException("Failed to adapt " + resource + " to " + modelClass.getSimpleName());
      }

      RhymeObjects.injectIntoSlingModel(slingModel, () -> new SlingRhymeImpl(this, resource));

      return slingModel;
    }
    catch (RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to adapt " + resource + " to " + modelClass.getSimpleName(), ex);
    }
  }

  private void verifyModelAnnotationIfPresent(Class<?> modelClass) {

    Model modelAnnotation = modelClass.getAnnotation(Model.class);
    if (modelAnnotation != null) {

      if (modelAnnotation.adaptables() == null) {
        throw new HalApiDeveloperException("The @" + Model.class.getSimpleName() + " annotation of " + modelClass + " does not define the adaptables");
      }

      if (!Arrays.asList(modelAnnotation.adaptables()).contains(Resource.class)) {
        throw new HalApiDeveloperException(modelClass + " is not declared to be adaptable from " + Resource.class);
      }
    }
  }

  @Override
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {

    if (UrlHandler.class.isAssignableFrom(type)) {
      return (AdapterType)request.adaptTo(UrlHandler.class);
    }
    if (Resource.class.isAssignableFrom(type)) {
      return (AdapterType)currentResource;
    }
    if (HttpServletRequest.class.isAssignableFrom(type)) {
      return (AdapterType)request;
    }

    return super.adaptTo(type);
  }

  @Override
  public HalResponse renderResource(LinkableResource resourceImpl) {

    return rhyme.renderResponse(resourceImpl);
  }

  @Override
  public RequestParameterMap getRequestParameters() {

    return request.getRequestParameterMap();
  }

  @Override
  public Resource getRequestedResource() {

    return request.getResource();
  }

  @Override
  public Resource getCurrentResource() {

    return currentResource;
  }

  Rhyme getCaravanRhyme() {

    return rhyme;
  }

  @Override
  public <T> T getRemoteResource(String uri, Class<T> halApiInterface) {
    return this.rhyme.getUpstreamEntryPoint(uri, halApiInterface);
  }

  @Override
  public void setResponseMaxAge(Duration duration) {
    this.rhyme.setResponseMaxAge(duration);
  }

}
