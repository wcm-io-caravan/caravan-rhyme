package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingRhyme.class, adapters = SlingLinkBuilder.class)
public class SlingLinkBuilderImpl implements SlingLinkBuilder {

  @Self
  private Resource targetResource;

  @Self
  private UrlHandler urlHandler;

  @Inject
  private ResourceSelectorRegistry registry;

  @Override
  public Link createLinkToCurrentResource(SlingLinkableResource slingModel) {

    String url = buildResourceUrl(slingModel);

    Link link = new Link(url)
        .setName(targetResource.getName())
        .setTitle(slingModel.getLinkTitle());

    return link;
  }

  private String buildResourceUrl(SlingLinkableResource slingModel) {

    String url = urlHandler.get(targetResource)
        .selectors(getClassSpecificSelector(slingModel))
        .extension(HalApiServlet.EXTENSION)
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

    return registry.getSelectorForModelClass(slingModel.getClass())
        .orElse(null);
  }

  @Override
  public <T extends LinkableResource> TemplateBuilder<T> buildTemplateTo(Class<T> halApiInterface) {

    return new TemplateBuilderImpl<T>(halApiInterface);
  }

  class TemplateBuilderImpl<T extends LinkableResource> implements TemplateBuilder<T> {

    private static final String PATH_PLACEHOLDER = "/letsassumethisisunlikelytoexist";
    private final Class<T> halApiInterface;

    private String linkTitle;
    private String[] queryParameters;

    TemplateBuilderImpl(Class<T> halApiInterface) {
      this.halApiInterface = halApiInterface;
    }

    @Override
    public Optional<T> buildOptional() {
      return Optional.of(createResource());
    }

    @Override
    public TemplateBuilder<T> withTitle(String title) {

      linkTitle = title;

      return this;
    }

    @Override
    public TemplateBuilder<T> withQueryParameters(String... parameters) {

      queryParameters = parameters;

      return this;
    }

    private <T extends LinkableResource> T createResource() {
      return (T)new LinkableResource() {

        @Override
        public Link createLink() {

          String baseTemplate = getResourceUrl().replace(PATH_PLACEHOLDER, "{+path}");

          UriTemplateBuilder builder = UriTemplate.buildFromTemplate(baseTemplate);

          if (queryParameters != null) {
            builder.query(queryParameters);
          }
          String uriTemplate = builder.build()
              .getTemplate();

          return new Link(uriTemplate)
              .setTitle(linkTitle);
        }

        private String getResourceUrl() {

          String selector = registry.getSelectorForHalApiInterface(halApiInterface).orElse(null);

          return urlHandler.get(PATH_PLACEHOLDER)
              .selectors(selector)
              .extension(HalApiServlet.EXTENSION)
              .buildExternalLinkUrl();
        }
      };
    }


  }
}
