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
import io.wcm.caravan.rhyme.aem.impl.SlingRhymeImpl;
import io.wcm.caravan.rhyme.aem.impl.parameters.QueryParamCollector;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingRhyme.class, adapters = SlingLinkBuilder.class)
public class SlingLinkBuilderImpl implements SlingLinkBuilder {

  private final Resource targetResource;

  private final UrlHandler urlHandler;

  private final RhymeResourceRegistry registry;

  /**
   * Default constructor used when this sling model is instantiated
   * @param targetResource the current sling resource (adapted via {@link SlingRhymeImpl#adaptTo(Class)} for which the
   *          link should be created
   * @param urlHandler an URL handler (adapted via {@link SlingRhymeImpl#adaptTo(Class)} to generate the resource URL
   * @param registry to lookup the selectors to be used for URL generation
   */
  @Inject
  public SlingLinkBuilderImpl(@Self Resource targetResource, @Self UrlHandler urlHandler, RhymeResourceRegistry registry) {
    this.targetResource = targetResource;
    this.urlHandler = urlHandler;
    this.registry = registry;
  }

  @Override
  public Link createLinkToCurrentResource(SlingLinkableResource slingModel) {

    String url = buildResourceUrl(slingModel);

    Link link = new Link(url)
        .setName(slingModel.getLinkProperties().getName())
        .setTitle(slingModel.getLinkProperties().getTitle());

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

    QueryParamCollector collector = new QueryParamCollector();

    Map<String, Object> queryParams = collector.getQueryParameters(slingModel);
    if (queryParams.isEmpty()) {
      return baseUrl;
    }
    String[] names = queryParams.keySet().toArray(new String[queryParams.size()]);

    UriTemplate template = UriTemplate.buildFromTemplate(baseUrl).query(names).build();

    queryParams.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .forEach(entry -> template.set(entry.getKey(), entry.getValue()));

    return slingModel.getLinkProperties().isTemplated() ? template.expandPartial() : template.expand();
  }

  private String getClassSpecificSelector(SlingLinkableResource slingModel) {

    return registry.getSelectorForModelClass(slingModel.getClass())
        .orElse(null);
  }

}
