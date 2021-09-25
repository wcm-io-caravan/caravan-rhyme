package io.wcm.caravan.rhyme.spring.impl;

import java.util.Map;

import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.spring.api.RhymeLinkBuilder;

/**
 * Returned by {@link UrlFingerprintingImpl#createLinkWith(WebMvcLinkBuilder)} to finish
 * building a link with URL fingerprinting.
 */
class SpringRhymeLinkBuilder implements RhymeLinkBuilder {

  private final Link link;
  private final Map<String, String> timestampParameters;

  private boolean withFingerprinting = true;

  SpringRhymeLinkBuilder(WebMvcLinkBuilder webMvcLinkBuilder, Map<String, String> timestampParameters) {

    this.link = new Link(webMvcLinkBuilder.toString());
    this.timestampParameters = timestampParameters;
  }

  @Override
  public SpringRhymeLinkBuilder withTitle(String title) {

    if (!link.isTemplated() || link.getTitle() == null) {
      link.setTitle(title);
    }
    return this;
  }

  @Override
  public SpringRhymeLinkBuilder withTemplateTitle(String title) {

    if (link.isTemplated()) {
      link.setTitle(title);
    }
    return this;
  }

  @Override
  public SpringRhymeLinkBuilder withName(String name) {

    link.setName(name);
    return this;
  }

  @Override
  public SpringRhymeLinkBuilder withFingerprintingOnlyIf(boolean value) {

    withFingerprinting = value;
    return this;
  }

  @Override
  public Link build() {

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(link.getHref());

    if (withFingerprinting) {
      timestampParameters.forEach(
          (name, value) -> uriBuilder.queryParam(name, value));
    }

    link.setHref(uriBuilder.build().toUriString());

    return link;
  }
}
