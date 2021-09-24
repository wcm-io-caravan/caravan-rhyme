package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import java.time.Duration;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponentsBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.spring.api.SpringRhyme;

@Component
@RequestScope
class TimestampedLinkBuilder {

  private static final String TIMESTAMP_QUERY_PARAM = "timestamp";

  private final String timestamp;
  private final boolean timestampPresentInRequest;

  TimestampedLinkBuilder(@Autowired RepositoryModificationListener rml, @Autowired WebRequest request, @Autowired SpringRhyme rhyme) {

    String timestampFromRequest = request.getParameter(TIMESTAMP_QUERY_PARAM);

    if (StringUtils.isNotBlank(timestampFromRequest)) {
      rhyme.setResponseMaxAge(Duration.ofDays(365));
      timestamp = timestampFromRequest;
      timestampPresentInRequest = true;
    }
    else {
      rhyme.setResponseMaxAge(Duration.ofSeconds(10));
      timestamp = rml.getLastModified().toString();
      timestampPresentInRequest = false;
    }
  }

  LinkBuildingStage create(WebMvcLinkBuilder linkBuilder) {
    return new LinkBuildingStage(linkBuilder);
  }

  <T> LinkBuildingStage createLinkTo(Class<T> controllerClass, Function<T, LinkableResource> controllerMethod) {
    return new LinkBuildingStage(WebMvcLinkBuilder.linkTo(controllerMethod.apply(WebMvcLinkBuilder.methodOn(controllerClass))));
  }

  boolean isTimeStampPresentInRequest() {
    return timestampPresentInRequest;
  }

  /**
   * Constructs an URL to the {@link CompanyApiController}, where a {@value #TIMESTAMP_QUERY_PARAM} parameter
   * is appended only if it was also present in the incoming request. This is used by the
   * {@link DetailedEmployeeController} to ensure that it can serve both immutable and mutable versions of the resource.
   * @return a fully qualified URL
   */
  String getLocalEntryPointUrl() {

    return createLinkTo(CompanyApiController.class, CompanyApiController::get)
        .withTimestamp(timestampPresentInRequest)
        .build()
        .getHref();
  }

  final class LinkBuildingStage {

    private final Link link;
    private boolean withTimestamp = true;

    private LinkBuildingStage(WebMvcLinkBuilder linkBuilder) {
      this.link = new Link(linkBuilder.toString());
    }

    public LinkBuildingStage withTitle(String title) {
      if (!link.isTemplated() || link.getTitle() == null) {
        link.setTitle(title);
      }
      return this;
    }

    public LinkBuildingStage withTemplateTitle(String title) {
      if (link.isTemplated()) {
        link.setTitle(title);
      }
      return this;
    }

    public LinkBuildingStage withName(String name) {
      link.setName(name);
      return this;
    }

    public LinkBuildingStage withTimestamp(boolean value) {
      withTimestamp = value;
      return this;
    }

    public Link build() {

      UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(link.getHref());

      if (withTimestamp) {
        uriBuilder.queryParam(TIMESTAMP_QUERY_PARAM, timestamp);
      }

      link.setHref(uriBuilder.build().toUriString());

      return link;
    }
  }
}
