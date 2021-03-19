package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.wcm.caravan.rhyme.aem.api.AemPage;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.impl.resources.AemPageImpl;
import io.wcm.caravan.rhyme.aem.impl.resources.SlingResourceImpl;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingHttpServletRequest.class)
public class SlingRhymeImpl extends SlingAdaptable implements SlingRhyme {


  private final SlingHttpServletRequest request;
  private final Resource currentResource;
  private final Rhyme rhyme;

  public SlingRhymeImpl(@Self SlingHttpServletRequest request) {

    System.out.println(this + " was instantiated");

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
      return null;
    }

    T slingModel = resource.adaptTo(modelClass);
    if (slingModel == null) {
      throw new RuntimeException("Failed to adapt " + resource + " to " + modelClass.getSimpleName());
    }

    RhymeObjects.injectIntoSlingModel(slingModel, () -> new SlingRhymeImpl(this, resource));

    return slingModel;
  }

  @Override
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {

    System.out.println("Trying to adapt " + this + " to " + type);
    if (UrlHandler.class.isAssignableFrom(type)) {
      return (AdapterType)request.adaptTo(UrlHandler.class);
    }
    if (Resource.class.isAssignableFrom(type)) {
      return (AdapterType)currentResource;
    }
    if (HttpServletRequest.class.isAssignableFrom(type)) {
      return (AdapterType)request;
    }
    System.out.println("Failed to adapt " + this + " to " + type);

    return super.adaptTo(type);
  }


  public HalResponse renderRequestedResource() {

    try {
      Resource requestedResource = getRequestedResource();

      LinkableResource resourceImpl = adaptToLinkableResource(requestedResource);

      return rhyme.renderResponse(resourceImpl);
    }
    catch (RuntimeException ex) {
      return rhyme.renderVndErrorResponse(ex);
    }
  }

  private LinkableResource adaptToLinkableResource(Resource requestedResource) {

    LinkableResource resourceImpl;

    List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());

    if (selectors.contains(AemPageImpl.SELECTOR)) {
      resourceImpl = adaptResource(requestedResource, AemPage.class);
    }
    if (selectors.contains(SlingResourceImpl.SELECTOR)) {
      resourceImpl = adaptResource(requestedResource, SlingResource.class);
    }
    else {
      resourceImpl = adaptResource(requestedResource, LinkableResource.class);
    }

    return resourceImpl;
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
}
