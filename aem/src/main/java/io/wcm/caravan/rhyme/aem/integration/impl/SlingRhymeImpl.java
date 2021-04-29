package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  public <@Nullable T> T adaptResource(Resource resource, @NotNull Class<T> modelClass) {

    if (resource == null) {
      throw new HalApiDeveloperException("Cannot adapt null resource to " + modelClass.getSimpleName());
    }

    T slingModel = resource.adaptTo(modelClass);
    if (slingModel == null) {
      throw new HalApiDeveloperException("Failed to adapt " + resource + " to " + modelClass.getSimpleName());
    }

    RhymeObjects.injectIntoSlingModel(slingModel, () -> new SlingRhymeImpl(this, resource));

    return slingModel;
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
  public HalResponse renderRequestedResource(Map<String, Class<? extends LinkableResource>> selectorModelClassMap) {

    try {
      Resource requestedResource = getRequestedResource();

      Class<? extends LinkableResource> slingModelClass = findSlingModelClass(selectorModelClassMap);

      LinkableResource resourceImpl = adaptResource(requestedResource, slingModelClass);

      return rhyme.renderResponse(resourceImpl);
    }
    catch (RuntimeException ex) {
      return rhyme.renderVndErrorResponse(ex);
    }
  }

  private Class<? extends LinkableResource> findSlingModelClass(Map<String, Class<? extends LinkableResource>> selectorModelClassMap) {

    List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());

    for (String selector : selectors) {
      Class<? extends LinkableResource> modelClass = selectorModelClassMap.get(selector);
      if (modelClass != null) {
        return modelClass;
      }
    }

    return LinkableResource.class;
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

}
