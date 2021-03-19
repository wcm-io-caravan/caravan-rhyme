package io.wcm.caravan.rhyme.aem.integration.impl;

import org.apache.sling.api.resource.Resource;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.handler.url.UrlHandler;

public class SlingLinkBuilderImpl implements SlingLinkBuilder {

  private final SlingRhyme rhyme;
  private final Resource targetResource;
  private final UrlHandler urlHandler;

  public SlingLinkBuilderImpl(SlingRhyme rhyme, UrlHandler urlHandler) {
    this.rhyme = rhyme;
    this.targetResource = rhyme.getCurrentResource();
    this.urlHandler = urlHandler;
  }

  @Override
  public Link createLinkToCurrentResource() {

    String url = buildResourceUrl();

    Link link = new Link(url)
        .setTitle(targetResource.getResourceType() + " via " + rhyme.getRequestParameters())
        .setName(targetResource.getName());

    return link;
  }

  private String buildResourceUrl() {

    return urlHandler.get(targetResource)
        .selectors(HalApiServlet.HAL_API_SELECTOR)
        .extension("json")
        .buildExternalResourceUrl();
  }
}
