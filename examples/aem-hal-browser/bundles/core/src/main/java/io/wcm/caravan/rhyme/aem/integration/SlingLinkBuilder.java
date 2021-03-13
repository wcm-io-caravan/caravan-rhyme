package io.wcm.caravan.rhyme.aem.integration;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = Resource.class, adapters = SlingLinkBuilder.class)
public class SlingLinkBuilder {

  @Self
  private Resource resource;
  @Self
  private UrlHandler urlHandler;

  public Link createLinkToCurrentResource() {

    String url = buildResourceUrl();

    Link link = new Link(url)
        .setTitle(resource.getResourceType())
        .setName(resource.getName());

    return link;

  }

  private String buildResourceUrl() {

    return urlHandler.get(resource)
        .selectors(HalApiServlet.HAL_API_SELECTOR)
        .extension("json")
        .buildExternalResourceUrl();
  }
}
