package io.wcm.caravan.rhyme.aem.integration;

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.url.UrlHandler;

public class SlingRhymeImpl implements SlingRhyme {

  private final SlingHttpServletRequest request;
  private final Resource currentResource;
  private final Rhyme rhyme;

  public SlingRhymeImpl(SlingHttpServletRequest request) {

    System.out.println(this + " was instantiated");

    this.request = request;

    this.currentResource = request.getResource();

    this.rhyme = RhymeBuilder.withoutResourceLoader()
        .buildForRequestTo(request.getRequestURL().toString());

  }

  private SlingRhymeImpl(SlingRhymeImpl slingRhyme, Resource currentResource) {

    this.request = slingRhyme.request;
    this.currentResource = currentResource;
    this.rhyme = slingRhyme.rhyme;
  }

  @Override
  public <T> T adaptResource(Resource resource, Class<T> modelClass) {

    if (resource == null) {
      return null;
    }

    T model = resource.adaptTo(modelClass);

    if (model == null) {
      throw new RuntimeException("Failed to adapt " + request + " to " + modelClass.getSimpleName());
    }

    try {
      FieldUtils.writeField(model, "rhyme", new SlingRhymeImpl(this, resource), true);
    }
    catch (IllegalAccessException ex) {
      throw new RuntimeException("Failed to inject new SlingRhymeImpl instance into " + model, ex);
    }


    return model;
  }


  public HalResponse renderRequestedResource() {

    try {
      LinkableResource resourceImpl = adaptResource(getRequestedResource(), LinkableResource.class);

      return rhyme.renderResponse(resourceImpl);
    }
    catch (RuntimeException ex) {
      return rhyme.renderVndErrorResponse(ex);
    }
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

  @Override
  public SlingLinkBuilder getLinkBuilder() {

    return new SlingLinkBuilderImpl(this, request.adaptTo(UrlHandler.class));
  }
}
