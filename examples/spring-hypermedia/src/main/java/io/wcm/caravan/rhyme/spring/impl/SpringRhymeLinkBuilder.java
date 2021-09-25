package io.wcm.caravan.rhyme.spring.impl;

import java.util.Map;

import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.spring.api.RhymeLinkBuilder;

class SpringRhymeLinkBuilder implements RhymeLinkBuilder {

  private final Map<String, String> timestampParameters;

  private final Link link;
  private boolean withTimestamps = true;

  SpringRhymeLinkBuilder(WebMvcLinkBuilder linkBuilder, Map<String, String> timestampParameters) {
    this.timestampParameters = timestampParameters;
    this.link = new Link(linkBuilder.toString());
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
    withTimestamps = value;
    return this;
  }

  @Override
  public Link build() {

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(link.getHref());

    if (withTimestamps) {
      timestampParameters.forEach(
          (name, value) -> uriBuilder.queryParam(name, value));
    }

    link.setHref(uriBuilder.build().toUriString());

    return link;
  }
}
