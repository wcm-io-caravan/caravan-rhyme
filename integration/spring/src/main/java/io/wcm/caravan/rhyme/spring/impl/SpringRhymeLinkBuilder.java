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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
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

    for (String name : queryParameterNames) {
      additionalQueryVariableNames.add(name);
    }
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

    UriComponents components = uriBuilder.build();
    String uri = components.toUriString();

    if (!additionalQueryVariableNames.isEmpty()) {

      String names = additionalQueryVariableNames.stream()
          .collect(Collectors.joining(","));

      String operator = components.getQuery() != null ? "&" : "?";

      uri = uri + "{" + operator + names + "}";
    }

    link.setHref(uri);

    return link;
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
  public <T extends LinkableResource> T buildProxyOf(Class<T> halApiInterface) {

    Class[] interfaces = Stream.of(halApiInterface, LinkableResource.class).toArray(Class[]::new);

    InvocationHandler handler = new InvocationHandler() {

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (!method.getName().equals("createLink") && method.getParameterCount() == 0) {
          throw new HalApiDeveloperException("Proxies created with RhymeLinkBuilder can only be used to call createLink on them");
        }

        return build();
      }
    };

    @SuppressWarnings("unchecked")
    T proxy = (T)Proxy.newProxyInstance(halApiInterface.getClassLoader(), interfaces, handler);

    return proxy;
  }
}
