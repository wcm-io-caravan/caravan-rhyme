package io.wcm.caravan.rhyme.aem.integration;

import java.util.Collections;
import java.util.Map;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

public abstract class AbstractLinkableResource implements LinkableResource, SlingLinkableResource {

  @RhymeObject
  protected SlingRhyme rhyme;

  @RhymeObject
  protected SlingResourceAdapter resourceAdapter;

  @RhymeObject
  private SlingLinkBuilder linkBuilder;

  private String contextLinkTitle;

  @Override
  public Link createLink() {

    return linkBuilder.createLinkToCurrentResource(this);
  }

  @Override
  public String getLinkTitle() {

    return contextLinkTitle != null ? contextLinkTitle : getDefaultLinkTitle();
  }

  public void setLinkTitle(String linkTitle) {

    this.contextLinkTitle = linkTitle;
  }

  protected abstract String getDefaultLinkTitle();

  @Override
  public Map<String, Object> getQueryParameters() {
    return Collections.emptyMap();
  }
}
