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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.ResourceConversions;
import io.wcm.caravan.rhyme.spring.api.RhymeLinkBuilder;

/**
 * Returned by {@link UrlFingerprintingImpl#createLinkWith(WebMvcLinkBuilder)} to finish
 * building a link with URL fingerprinting.
 */
class SpringRhymeLinkBuilder implements RhymeLinkBuilder {

  private final Link link;
  private final Map<String, String> fingerprintingParameters;
  private final Map<String, Object> additionalQueryParameters;

  private final List<String> additionalQueryVariableNames = new ArrayList<>();

  private boolean withFingerprinting = true;

  SpringRhymeLinkBuilder(WebMvcLinkBuilder webMvcLinkBuilder, Map<String, String> fingerprintingParameters, Map<String, Object> stickyParameters) {

    this.link = new Link(webMvcLinkBuilder.toString());

    this.fingerprintingParameters = fingerprintingParameters;
    this.additionalQueryParameters = stickyParameters;
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
  public RhymeLinkBuilder withTemplateVariables(String... queryParameterNames) {

    additionalQueryVariableNames.addAll(Arrays.asList(queryParameterNames));
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

    appendQueryParams(uriBuilder);

    if (withFingerprinting) {
      fingerprintingParameters.forEach(uriBuilder::queryParam);
    }

    if (additionalQueryVariableNames.isEmpty()) {
      return link.setHref(uriBuilder.build().toString());
    }

    return link.setHref(buildTemplateWithAdditionalVariables(uriBuilder));

  }

  private String buildTemplateWithAdditionalVariables(UriComponentsBuilder uriBuilder) {

    stripQueryParams(uriBuilder, additionalQueryVariableNames);

    UriComponents components = uriBuilder.build();

    String names = additionalQueryVariableNames.stream()
        .collect(Collectors.joining(","));

    String operator = components.getQuery() != null ? "&" : "?";

    return components.toUriString() + "{" + operator + names + "}";
  }

  private static void stripQueryParams(UriComponentsBuilder uriBuilder, List<String> namesToStrip) {

    UriComponents components = uriBuilder.build();

    MultiValueMap<String, String> existingParams = components.getQueryParams();
    MultiValueMap<String, String> strippedParams = new LinkedMultiValueMap<>();

    existingParams.keySet().stream()
        .filter(name -> !namesToStrip.contains(name))
        .forEach(name -> strippedParams.put(name, existingParams.get(name)));

    uriBuilder.replaceQueryParams(strippedParams);
  }

  private void appendQueryParams(UriComponentsBuilder uriBuilder) {

    MultiValueMap<String, String> existingParams = uriBuilder.build().getQueryParams();

    additionalQueryParameters.forEach((name, value) -> {
      if (!existingParams.containsKey(name) && !additionalQueryVariableNames.contains(name)) {
        uriBuilder.queryParam(name, value);
      }
    });
  }

  @Override
  public <T extends LinkableResource> T buildLinked(Class<T> halApiInterface) {

    Link linkToResource = build();

    return ResourceConversions.asLinkableResource(linkToResource, halApiInterface);
  }
}
