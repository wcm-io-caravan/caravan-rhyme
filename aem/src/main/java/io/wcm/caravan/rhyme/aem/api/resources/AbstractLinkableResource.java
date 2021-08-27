package io.wcm.caravan.rhyme.aem.api.resources;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.models.annotations.injectorspecific.Self;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.SlingLinkBuilder;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * An abstract implementation of {@link LinkableResource}, that you can use as a super class
 * for your sling models implementing your {@link HalApiInterface}s. It provides injected fields with
 * the commonly used {@link SlingRhyme} and {@link SlingResourceAdapter} sling models, and
 * also contains an implementation of {@link LinkableResource#createLink()} that uses
 * the {@link SlingLinkBuilder} to generate the link.
 */
public abstract class AbstractLinkableResource implements LinkableResource, SlingLinkableResource {

  @Self
  protected SlingRhyme rhyme;

  @Self
  protected SlingResourceAdapter resourceAdapter;

  @Self
  protected SlingLinkBuilder linkBuilder;

  private String contextLinkTitle;
  private String linkName;

  private boolean useParametersFromRequest = true;
  private Map<String, Object> queryParameters = new LinkedHashMap<>();

  private boolean expandAllVariables = true;

  @Self
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

  @Override
  public String getLinkName() {

    if (linkName != null) {
      return linkName;
    }

    if (rhyme != null) {
      return rhyme.getCurrentResource().getName();
    }

    return null;
  }

  @Override
  public void setLinkName(String name) {

    this.linkName = name;
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

  @Override
  public void setExpandAllVariables(boolean expandAllVariables) {
    this.expandAllVariables = expandAllVariables;
  }

  @Override
  public boolean isExpandAllVariables() {
    return expandAllVariables;
  }


}
