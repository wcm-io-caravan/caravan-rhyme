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
package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.Duration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.spring.api.RhymeLinkBuilder;
import io.wcm.caravan.rhyme.spring.api.SpringRhyme;
import io.wcm.caravan.rhyme.spring.api.UrlFingerprinting;

/**
 * Defines common logic (e.g. URL fingerprinting) for building any link to your resources in this project .
 * <p>
 * In general, the core {@link Rhyme} framework only specifies that the {@link LinkableResource#createLink()}
 * of each server-side resource instance is responsible to create a link for itself that contains all parameters and
 * context information. But often there is some common logic required to build links that is shared throughout the
 * project, and having a class like this helps to consolidate and encapsulate that kind of logic.
 * </p>
 * <p>
 * In this case links are mostly generated with Spring HATEOAS' {@link WebMvcLinkBuilder}, but additional logic is
 * added by the {@link UrlFingerprinting} class: Each link will contain a {@value #TIMESTAMP_QUERY_PARAM} query
 * parameter that contains the last modification date of all the data used to generate the resources. This query
 * parameter has a single purpose: if it is present, we can instruct clients that responses for this URL can be cached
 * essentially forever. This is because future requests will be using a different URL with an updated timestamp
 * if data has changed. All this is implemented by {@link UrlFingerprinting} and we just need to enable and
 * configure it here.
 * </p>
 * @see UrlFingerprinting
 * @see WebMvcLinkBuilder
 */
@Component
@RequestScope
class CompanyApiLinkBuilder {

  private static final String TIMESTAMP_QUERY_PARAM = "timestamp";

  private final UrlFingerprinting fingerprinting;

  private Boolean useEmbeddedResources;
  private Boolean useFingerprinting;

  CompanyApiLinkBuilder(@Autowired SpringRhyme rhyme, @Autowired RepositoryModificationListener repositoryListener,
      @Autowired HttpServletRequest request) {

    this.fingerprinting = rhyme
        .enableUrlFingerprinting()
        .withConditionalMaxAge(Duration.ofSeconds(10), Duration.ofDays(100))
        .withTimestampParameter(TIMESTAMP_QUERY_PARAM, repositoryListener::getLastModified);

    useEmbeddedResources = initialiseStickySettingsParmeter(request, CompanyApi.USE_EMBEDDED_RESOURCES);
    useFingerprinting = initialiseStickySettingsParmeter(request, CompanyApi.USE_FINGERPRINTING);
  }

  private Boolean initialiseStickySettingsParmeter(HttpServletRequest request, String name) {

    // if the query parameter was not present in the incoming request, then don't add it to any other links
    String fromRequest = request.getParameter(name);
    if (StringUtils.isBlank(fromRequest)) {
      // but for both parameters, the default behavior should be as if this parameter was set to true
      return true;
    }

    // if the parameter *was* present, then make sure it's also added to every other link
    Boolean boolValue = BooleanUtils.toBoolean(fromRequest);
    fingerprinting.addQueryParameter(name, boolValue);
    return boolValue;
  }

  boolean isUseFingerprinting() {
    return useFingerprinting;
  }

  boolean isUseEmbeddedResources() {
    return useEmbeddedResources;
  }

  /**
   * Start building a link to a controller handler method. The URL for that link is generated
   * by the {@link WebMvcLinkBuilder} class, but additional query parameters that are not directly
   * present in the API and controller signatures can be appended by {@link UrlFingerprinting}.
   * @param webMvcLinkBuilder created with {@link WebMvcLinkBuilder#linkTo(Class)} and
   *          {@link WebMvcLinkBuilder#methodOn(Class, Object...)}
   * @return a {@link RhymeLinkBuilder} that you can use to decorate the link with name and title attributes, and
   *         then finally build it
   * @see WebMvcLinkBuilder
   */
  RhymeLinkBuilder create(WebMvcLinkBuilder webMvcLinkBuilder) {

    RhymeLinkBuilder linkBuilder = fingerprinting.createLinkWith(webMvcLinkBuilder);

    if (!isUseFingerprinting()) {
      linkBuilder = linkBuilder.withoutFingerprint();
    }
    return linkBuilder;
  }

  /**
   * Constructs an URL to the {@link CompanyApiController}, where a {@value #TIMESTAMP_QUERY_PARAM} parameter
   * is appended only if it was also present in the incoming request. This is used by the
   * {@link DetailedEmployeeController} to ensure that it can serve both immutable and mutable versions of the resource.
   * @return a fully qualified URL for requests to the {@link CompanyApiController#get()} method
   */
  String getLocalEntryPointUrl() {

    RhymeLinkBuilder linkBuilder = create(linkTo(methodOn(CompanyApiController.class).get()));

    if (!isUseFingerprinting() || !fingerprinting.isUsedInIncomingRequest()) {
      linkBuilder = linkBuilder.withoutFingerprint();
    }

    return linkBuilder.build()
        .getHref();
  }
}
