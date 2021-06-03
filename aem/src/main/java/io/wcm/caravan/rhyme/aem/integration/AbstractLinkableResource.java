package io.wcm.caravan.rhyme.aem.integration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

public abstract class AbstractLinkableResource implements LinkableResource, SlingLinkableResource {

  @RhymeObject
  protected SlingRhyme rhyme;

  @RhymeObject
  protected SlingResourceAdapter resourceAdapter;

  @RhymeObject
  protected SlingLinkBuilder linkBuilder;

  private String contextLinkTitle;

  private boolean useParametersFromRequest = true;
  private Map<String, Object> queryParameters = new LinkedHashMap<>();

  @RhymeObject
  private SlingHttpServletRequest request;


  @Override
  public Link createLink() {

    return linkBuilder.createLinkToCurrentResource(this);
  }

  @Override
  public String getLinkTitle() {

    return contextLinkTitle != null ? contextLinkTitle : getDefaultLinkTitle();
  }

  @Override
  public void setLinkTitle(String linkTitle) {

    this.contextLinkTitle = linkTitle;
  }

  protected abstract String getDefaultLinkTitle();

  @Override
  public Map<String, Object> getQueryParameters() {

    if (useParametersFromRequest) {
      for (RequestParameter parameter : request.getRequestParameterList()) {
        // TODO: add support for parameters with multiple values
        queryParameters.put(parameter.getName(), parameter.getString());
      }
      useParametersFromRequest = false;
    }

    return queryParameters;
  }

  @Override
  public void setQueryParameters(Map<String, Object> parameters) {
    useParametersFromRequest = false;
    queryParameters = parameters;
  }
}
