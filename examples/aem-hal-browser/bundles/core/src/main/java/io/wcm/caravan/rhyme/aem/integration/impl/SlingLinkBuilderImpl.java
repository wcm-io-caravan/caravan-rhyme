package io.wcm.caravan.rhyme.aem.integration.impl;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingRhyme.class, adapters = SlingLinkBuilder.class)
public class SlingLinkBuilderImpl implements SlingLinkBuilder {

  private static final String SELECTOR_CONSTANT = "SELECTOR";

  @Self
  private SlingHttpServletRequest request;

  @Self
  private Resource targetResource;

  @Self
  private UrlHandler urlHandler;

  @Override
  public Link createLinkToCurrentResource(LinkableResource slingModel) {

    String url = buildResourceUrl(slingModel);

    Link link = new Link(url)
        .setTitle(targetResource.getResourceType())
        .setName(targetResource.getName());

    return link;
  }

  private String buildResourceUrl(LinkableResource slingModel) {

    String classSpecificSelector = getClassSpecificSelector(slingModel);

    return urlHandler.get(targetResource)
        .selectors(HalApiServlet.HAL_API_SELECTOR + "." + classSpecificSelector)
        .extension("json")
        .buildExternalResourceUrl();
  }

  private String getClassSpecificSelector(LinkableResource slingModel) {
    try {
      return slingModel.getClass().getField(SELECTOR_CONSTANT).get(null).toString();
    }
    catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ex) {
      throw new HalApiDeveloperException("Failed to read value from static field " + SELECTOR_CONSTANT + " of class " + slingModel.getClass());
    }
  }
}
