/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.reha.impl.renderer;

import static io.wcm.caravan.reha.api.relations.StandardRelations.VIA;
import static io.wcm.caravan.reha.api.relations.VndErrorRelations.ABOUT;
import static io.wcm.caravan.reha.api.relations.VndErrorRelations.ERRORS;
import static io.wcm.caravan.reha.impl.renderer.AsyncHalResponseRendererImpl.addMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.common.HalResponse;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.exceptions.HalApiClientException;
import io.wcm.caravan.reha.api.relations.StandardRelations;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.reha.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.reha.impl.metadata.ResponseMetadataRelations;

/**
 * Full implementation of {@link VndErrorResponseRenderer}
 */
public class VndErrorResponseRendererImpl implements VndErrorResponseRenderer {

  private static final Logger log = LoggerFactory.getLogger(VndErrorResponseRendererImpl.class);

  private static final DefaultExceptionStatusAndLoggingStrategy DEFAULT_STRATEGY = new DefaultExceptionStatusAndLoggingStrategy();

  private final ExceptionStatusAndLoggingStrategy strategy;

  /**
   * @param customStrategy allows to control the status code and logging of exceptions
   */
  public VndErrorResponseRendererImpl(ExceptionStatusAndLoggingStrategy customStrategy) {
    this.strategy = DEFAULT_STRATEGY.decorateWith(customStrategy);
  }

  @Override
  public HalResponse renderError(String requestUri, LinkableResource resourceImpl, Throwable error, RequestMetricsCollector metrics) {

    HalResource vndResource = new HalResource();

    addProperties(vndResource, error);
    addAboutAndCanonicalLink(vndResource, resourceImpl, requestUri);
    addEmbeddedCauses(vndResource, error);
    addMetadata(metrics, vndResource, resourceImpl);

    int status = ObjectUtils.defaultIfNull(strategy.extractStatusCode(error), 500);

    logError(error, requestUri, status);

    return new HalResponse()
        .withStatus(status)
        .withContentType(VndErrorResponseRenderer.CONTENT_TYPE)
        .withBody(vndResource);
  }

  private String getShortErrorMessage(Throwable t) {
    return strategy.getErrorMessageWithoutRedundantInformation(t);
  }

  private void logError(Throwable error, String uri, int status) {

    if (strategy.logAsCompactWarning(error)) {
      // if this error was caused by an upstream request, there is no need to include the full stack traces
      String messages = Stream.of(ExceptionUtils.getThrowables(error))
          .map(t -> t.getClass().getSimpleName() + ": " + getShortErrorMessage(t))
          .collect(Collectors.joining("\n"));

      log.warn("Responding with " + status + " for " + uri + ":\n" + messages);
    }
    else {
      log.error("Responding with " + status + " for " + uri, error);
    }
  }

  private void addProperties(HalResource vndResource, Throwable error) {

    vndResource.getModel().put("message", getShortErrorMessage(error));
    vndResource.getModel().put("class", error.getClass().getName());
    vndResource.getModel().put("title", error.getClass().getSimpleName() + ": " + getShortErrorMessage(error));
  }

  private void addAboutAndCanonicalLink(HalResource vndResource, LinkableResource resourceImpl, String requestUri) {

    Link aboutLink = new Link(requestUri).setTitle("The URI of this resource as it was actually requested");
    vndResource.addLinks(ABOUT, aboutLink);

    Link selfLink = null;
    try {
      selfLink = resourceImpl.createLink()
          .setTitle("The URI as reported by the self-link of this resource");
    }
    // CHECKSTYLE:OFF - we really want to ignore any exceptions that could be thrown when creating the self-link
    catch (RuntimeException ex) {
      // CHECKSTYLE:ON
    }

    if (selfLink != null) {
      if (!requestUri.endsWith(selfLink.getHref())) {
        vndResource.addLinks(StandardRelations.CANONICAL, selfLink);
      }
    }
  }

  private void addEmbeddedCauses(HalResource vndResource, Throwable error) {

    Throwable cause = error.getCause();
    if (cause != null) {
      HalResource embedded = new HalResource();
      addProperties(embedded, cause);

      vndResource.addEmbedded(ERRORS, embedded);
      addEmbeddedCauses(vndResource, cause);

      if (cause instanceof HalApiClientException) {
        List<HalResource> vndErrorsFoundInBody = getErrorsFromUpstreamResponse(vndResource, (HalApiClientException)cause);
        vndResource.addEmbedded(ERRORS, vndErrorsFoundInBody);
      }
    }
  }

  private List<HalResource> getErrorsFromUpstreamResponse(HalResource context, HalApiClientException cause) {

    HalApiClientException hace = cause;

    Link link = new Link(hace.getRequestUrl()).setTitle("The upstream resource that could not be loaded");

    context.addLinks(VIA, link);

    HalResponse upstreamJson = hace.getErrorResponse();
    HalResource upstreamBody = upstreamJson.getBody();

    if (upstreamBody == null || upstreamBody.getModel().size() == 0) {
      return Collections.emptyList();
    }

    HalResource causeFromBody = new HalResource(upstreamBody.getModel().deepCopy());
    causeFromBody.removeEmbedded(ResponseMetadataRelations.CARAVAN_METADATA_RELATION);

    List<HalResource> embeddedCauses = causeFromBody.getEmbedded(ERRORS);

    List<HalResource> flatCauses = new ArrayList<>();
    flatCauses.add(causeFromBody);
    flatCauses.addAll(embeddedCauses);

    causeFromBody.removeEmbedded(ERRORS);
    return flatCauses;
  }
}
