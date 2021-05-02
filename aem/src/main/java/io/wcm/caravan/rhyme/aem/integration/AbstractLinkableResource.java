package io.wcm.caravan.rhyme.aem.integration;

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

}
