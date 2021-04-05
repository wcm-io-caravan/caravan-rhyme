package io.wcm.caravan.rhyme.aem.integration;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

public abstract class AbstractLinkableResource implements LinkableResource {

  @RhymeObject
  protected SlingResourceAdapter resourceAdapter;

  @RhymeObject
  private SlingLinkBuilder linkBuilder;

  private String contextLinkTitle;

  @Override
  public Link createLink() {

    String linkTitle = contextLinkTitle != null ? contextLinkTitle : getDefaultLinkTitle();

    return linkBuilder.createLinkToCurrentResource(this)
        .setTitle(linkTitle);
  }

  public void setLinkTitle(String linkTitle) {

    this.contextLinkTitle = linkTitle;
  }

  protected abstract String getDefaultLinkTitle();

}
