package io.wcm.caravan.rhyme.aem.impl.linkbuilder;

import java.util.Map;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.damnhandy.uri.template.UriTemplate;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.api.resources.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.impl.HalApiServlet;
import io.wcm.caravan.rhyme.aem.impl.RhymeResourceRegistry;
import io.wcm.caravan.rhyme.aem.impl.SlingRhymeImpl;
import io.wcm.caravan.rhyme.aem.impl.parameters.QueryParamCollector;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingRhymeImpl.class, adapters = SlingLinkBuilder.class)
public class SlingLinkBuilderImpl implements SlingLinkBuilder {

  private final Resource targetResource;

  private final UrlHandler urlHandler;

  private final RhymeResourceRegistry registry;

  /**
   * Default constructor used when this sling model is instantiated
   * @param slingRhyme the model from which this link builder is adapted
   * @param registry to lookup the selectors to be used for URL generation
   */
  @Inject
  public SlingLinkBuilderImpl(@Self SlingRhymeImpl slingRhyme, RhymeResourceRegistry registry) {
    this.targetResource = slingRhyme.getCurrentResource();
    this.urlHandler = slingRhyme.getUrlHandler();
    this.registry = registry;
  }

  @Override
  public Link createLinkToCurrentResource(SlingLinkableResource slingModel) {

    String url = buildResourceUrl(slingModel);

    return new Link(url)
        .setName(slingModel.getLinkProperties().getName())
        .setTitle(slingModel.getLinkProperties().getTitle());
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

    // Workaround for a Java 21 issue: the UriTemplate class can no longer expand
    // a query parameter template with an empty list/array value.
    // Skip empty collections from both the template variable names and the value map.
    String[] names = queryParams.entrySet().stream()
        .filter(entry -> !isEmptyCollection(entry.getValue()))
        .map(Map.Entry::getKey)
        .toArray(String[]::new);

    if (names.length == 0) {
      return baseUrl;
    }

    UriTemplate template = UriTemplate.buildFromTemplate(baseUrl).query(names).build();

    queryParams.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .filter(entry -> !isEmptyCollection(entry.getValue()))
        .forEach(entry -> template.set(entry.getKey(), entry.getValue()));

    return slingModel.getLinkProperties().isTemplated() ? template.expandPartial() : template.expand();
  }

  private static boolean isEmptyCollection(Object value) {
    if (value instanceof Iterable) {
      return !((Iterable<?>)value).iterator().hasNext();
    }
    if (value != null && value.getClass().isArray()) {
      return java.lang.reflect.Array.getLength(value) == 0;
    }
    return false;
  }

  private String getClassSpecificSelector(SlingLinkableResource slingModel) {

    return registry.getSelectorForModelClass(slingModel.getClass())
        .orElse(null);
  }

}
