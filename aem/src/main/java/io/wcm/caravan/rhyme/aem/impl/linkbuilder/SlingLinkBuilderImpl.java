package io.wcm.caravan.rhyme.aem.impl.linkbuilder;

import java.util.Map;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.damnhandy.uri.template.UriTemplate;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.api.resources.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.impl.HalApiServlet;
import io.wcm.caravan.rhyme.aem.impl.RhymeResourceRegistry;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingRhyme.class, adapters = SlingLinkBuilder.class)
public class SlingLinkBuilderImpl implements SlingLinkBuilder {

  @Self
  private Resource targetResource;

  @Self
  private UrlHandler urlHandler;

  @Inject
  private RhymeResourceRegistry registry;

  @Override
  public Link createLinkToCurrentResource(SlingLinkableResource slingModel) {

    String url = buildResourceUrl(slingModel);

    Link link = new Link(url)
        .setName(slingModel.getLinkName())
        .setTitle(slingModel.getLinkTitle());

    return link;
  }

  private String buildResourceUrl(SlingLinkableResource slingModel) {

    String url = urlHandler.get(targetResource)
        .selectors(getClassSpecificSelector(slingModel))
        .extension(HalApiServlet.EXTENSION)
        .build();

    return appendQueryWithTemplate(url, slingModel);
  }

  private String appendQueryWithTemplate(String baseUrl, SlingLinkableResource slingModel) {

    Map<String, Object> queryParams = slingModel.getQueryParameters();
    if (queryParams.isEmpty()) {
      return baseUrl;
    }
    String[] names = queryParams.keySet().toArray(new String[queryParams.size()]);

    UriTemplate template = UriTemplate.buildFromTemplate(baseUrl).query(names).build();

    queryParams.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .forEach(entry -> template.set(entry.getKey(), entry.getValue()));

    return slingModel.isExpandAllVariables() ? template.expand() : template.expandPartial();
  }

  private String getClassSpecificSelector(SlingLinkableResource slingModel) {

    return registry.getSelectorForModelClass(slingModel.getClass())
        .orElse(null);
  }

}
