package io.wcm.caravan.rhyme.aem.integration;

import java.util.Optional;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;


public interface SlingLinkBuilder {

  Link createLinkToCurrentResource(SlingLinkableResource slingModel);

  <T extends LinkableResource> TemplateBuilder<T> buildTemplateTo(Class<T> halApiInterface);

  interface TemplateBuilder<T extends LinkableResource> {

    TemplateBuilder<T> withTitle(String title);

    TemplateBuilder<T> withQueryParameters(String... parameters);

    Optional<T> buildOptional();
  }
}
