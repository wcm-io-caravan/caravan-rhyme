/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
  private final Map<String, String> fingerprintingParameters;

  private boolean withFingerprinting = true;

  SpringRhymeLinkBuilder(WebMvcLinkBuilder webMvcLinkBuilder, Map<String, String> fingerprintingParameters) {

    this.link = new Link(webMvcLinkBuilder.toString());
    this.fingerprintingParameters = fingerprintingParameters;
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
  public SpringRhymeLinkBuilder withoutFingerprint() {

    withFingerprinting = false;
    return this;
  }

  @Override
  public Link build() {

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(link.getHref());

    if (withFingerprinting) {
      fingerprintingParameters.forEach(uriBuilder::queryParam);
    }

    link.setHref(uriBuilder.build().toUriString());

    return link;
  }
}
