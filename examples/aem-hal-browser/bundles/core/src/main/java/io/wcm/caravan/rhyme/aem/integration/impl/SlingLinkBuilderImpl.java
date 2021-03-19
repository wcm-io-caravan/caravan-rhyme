package io.wcm.caravan.rhyme.aem.integration.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingRhyme.class, adapters = SlingLinkBuilder.class)
public class SlingLinkBuilderImpl implements SlingLinkBuilder {

  @Self
  private SlingHttpServletRequest request;

  @Self
  private Resource targetResource;

  @Self
  private UrlHandler urlHandler;

  @Override
  public Link createLinkToCurrentResource() {

    String url = buildResourceUrl();

    Link link = new Link(url)
        .setTitle(targetResource.getResourceType() + " via " + request.getRequestParameterMap())
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
