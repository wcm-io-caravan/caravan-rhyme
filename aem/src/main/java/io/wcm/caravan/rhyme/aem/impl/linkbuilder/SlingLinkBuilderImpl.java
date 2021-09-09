package io.wcm.caravan.rhyme.aem.impl.linkbuilder;

import static io.wcm.caravan.rhyme.aem.impl.HalApiServlet.QUERY_PARAM_EMBED_METADATA;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.damnhandy.uri.template.UriTemplate;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.api.resources.ImmutableResource;
import io.wcm.caravan.rhyme.aem.api.resources.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.impl.HalApiServlet;
import io.wcm.caravan.rhyme.aem.impl.RhymeResourceRegistry;
import io.wcm.caravan.rhyme.aem.impl.SlingRhymeImpl;
import io.wcm.caravan.rhyme.aem.impl.parameters.QueryParamCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingRhymeImpl.class, adapters = SlingLinkBuilder.class)
public class SlingLinkBuilderImpl implements SlingLinkBuilder {

  private final Resource targetResource;

  private final UrlHandler urlHandler;

  private final RhymeResourceRegistry registry;

  private final SlingRhymeImpl rhyme;

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
    this.rhyme = slingRhyme;
  }

  @Override
  public Link createLinkToCurrentResource(SlingLinkableResource slingModel) {

    String url = buildResourceUrlOrTemplate(slingModel);

    Link link = new Link(url)
        .setName(slingModel.getLinkProperties().getName())
        .setTitle(slingModel.getLinkProperties().getTitle());

    return link;
  }

  private String buildResourceUrlOrTemplate(SlingLinkableResource slingModel) {

    String baseUrl = urlHandler.get(targetResource)
        .selectors(getClassSpecificSelector(slingModel))
        .extension(HalApiServlet.EXTENSION)
        .build();

    Map<String, Object> allParams = createQueryParameterMap(slingModel);
    if (allParams.isEmpty()) {
      return baseUrl;
    }

    UriTemplate template = createLinkTemplate(baseUrl, allParams);

    return slingModel.getLinkProperties().isTemplated() ? template.expandPartial() : template.expand();
  }

  private UriTemplate createLinkTemplate(String baseUrl, Map<String, Object> queryParams) {

    String[] names = queryParams.keySet().toArray(new String[queryParams.size()]);

    UriTemplate template = UriTemplate.buildFromTemplate(baseUrl).query(names).build();

    queryParams.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .forEach(entry -> template.set(entry.getKey(), entry.getValue()));

    return template;
  }

  private Map<String, Object> createQueryParameterMap(SlingLinkableResource slingModel) {

    Map<String, Object> allParams = new LinkedHashMap<String, Object>();

    // keep the ?embedMetadata parameter if it was speciied in the incoming request
    if (rhyme.getRequest().getParameterMap().containsKey(QUERY_PARAM_EMBED_METADATA)) {
      allParams.put(QUERY_PARAM_EMBED_METADATA, true);
    }

    allParams.putAll(getFingerprintingParams(slingModel));
    allParams.putAll(getAnnotatedParams(slingModel));

    return allParams;
  }

  private Map<String, Object> getAnnotatedParams(SlingLinkableResource slingModel) {

    QueryParamCollector collector = new QueryParamCollector();

    return collector.getQueryParameters(slingModel);
  }

  private Map<String, Object> getFingerprintingParams(SlingLinkableResource slingModel) {

    if (!(slingModel instanceof ImmutableResource)) {
      return Collections.emptyMap();
    }

    try (RequestMetricsStopwatch sw = rhyme.getCoreRhyme().startStopwatch(SlingLinkBuilderImpl.class, () -> "adding fingerprint parameter")) {

      UrlFingerprintingImpl fingerprinting = rhyme.adaptTo(UrlFingerprintingImpl.class);

      return fingerprinting.getQueryParams(slingModel);
    }
  }

  private String getClassSpecificSelector(SlingLinkableResource slingModel) {

    return registry.getSelectorForModelClass(slingModel.getClass())
        .orElse(null);
  }

}
