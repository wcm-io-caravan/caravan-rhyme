package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.damnhandy.uri.template.UriTemplate;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingRhyme.class, adapters = SlingLinkBuilder.class)
public class SlingLinkBuilderImpl implements SlingLinkBuilder {

  private static final String SELECTOR_CONSTANT = "SELECTOR";

  @Self
  private Resource targetResource;

  @Self
  private UrlHandler urlHandler;

  @Override
  public Link createLinkToCurrentResource(SlingLinkableResource slingModel) {

    String url = buildResourceUrl(slingModel);

    Link link = new Link(url)
        .setName(targetResource.getName())
        .setTitle(slingModel.getLinkTitle());

    return link;
  }

  private String buildResourceUrl(SlingLinkableResource slingModel) {

    String classSpecificSelector = getClassSpecificSelector(slingModel);

    String url = urlHandler.get(targetResource)
        .selectors(HalApiServlet.HAL_API_SELECTOR + "." + classSpecificSelector)
        .extension("json")
        .buildExternalResourceUrl();

    return appendQueryWithTemplate(url, slingModel);
  }

  private String appendQueryWithTemplate(String baseUrl, SlingLinkableResource slingModel) {

    Map<String, Object> suffixParts = slingModel.getQueryParameters();
    if (suffixParts.isEmpty()) {
      return baseUrl;
    }
    String[] names = suffixParts.keySet().toArray(new String[suffixParts.size()]);

    UriTemplate template = UriTemplate.buildFromTemplate(baseUrl).query(names).build();

    suffixParts.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .forEach(entry -> template.set(entry.getKey(), entry.getValue()));

    return template.expandPartial();
  }

  private String getClassSpecificSelector(SlingLinkableResource slingModel) {
    try {
      return slingModel.getClass().getField(SELECTOR_CONSTANT).get(null).toString();
    }
    catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ex) {
      throw new HalApiDeveloperException("Failed to read value from static field " + SELECTOR_CONSTANT + " of class " + slingModel.getClass(), ex);
    }
  }
}
