package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import java.time.Duration;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.spring.api.SpringRhyme;
import io.wcm.caravan.rhyme.spring.impl.UrlFingerprinting;
import io.wcm.caravan.rhyme.spring.impl.SpringRhymeLinkBuilder;

@Component
@RequestScope
class TimestampedLinkBuilder {

  private static final String TIMESTAMP_QUERY_PARAM = "timestamp";

  private final UrlFingerprinting fingerprinting;

  TimestampedLinkBuilder(@Autowired SpringRhyme rhyme, @Autowired RepositoryModificationListener repositoryListener) {

    this.fingerprinting = rhyme
        .enableUrlFingerprinting()
        .withMutableMaxAge(Duration.ofSeconds(10))
        .withImmutableMaxAge(Duration.ofDays(30))
        .withTimestampParameter(TIMESTAMP_QUERY_PARAM, repositoryListener::getLastModified);
  }

  SpringRhymeLinkBuilder create(WebMvcLinkBuilder linkBuilder) {

    return fingerprinting.createLinkWith(linkBuilder);
  }

  <T> SpringRhymeLinkBuilder createLinkTo(Class<T> controllerClass, Function<T, LinkableResource> controllerMethod) {

    return create(WebMvcLinkBuilder.linkTo(controllerMethod.apply(WebMvcLinkBuilder.methodOn(controllerClass))));
  }

  /**
   * Constructs an URL to the {@link CompanyApiController}, where a {@value #TIMESTAMP_QUERY_PARAM} parameter
   * is appended only if it was also present in the incoming request. This is used by the
   * {@link DetailedEmployeeController} to ensure that it can serve both immutable and mutable versions of the resource.
   * @return a fully qualified URL
   */
  String getLocalEntryPointUrl() {

    return createLinkTo(CompanyApiController.class, CompanyApiController::get)
        .withTimestamps(fingerprinting.isFingerprintPresentInRequest())
        .build()
        .getHref();
  }
}
