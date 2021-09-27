package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.Duration;
import java.util.function.Function;

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

  CompanyApiLinkBuilder(@Autowired SpringRhyme rhyme, @Autowired RepositoryModificationListener repositoryListener) {

    this.fingerprinting = rhyme
        .enableUrlFingerprinting()
        .withConditionalMaxAge(Duration.ofSeconds(10), Duration.ofDays(100))
        .withTimestampParameter(TIMESTAMP_QUERY_PARAM, repositoryListener::getLastModified);
  }

  /**
   * Start building a link to a controller handler method. The URL for that link is generated
   * by the {@link WebMvcLinkBuilder} class, but additional query parameters that are not directly
   * present in the API and controller signatures can be appended by {@link UrlFingerprinting}.
   * @param linkBuilder created with {@link WebMvcLinkBuilder#linkTo(Class)} and
   *          {@link WebMvcLinkBuilder#methodOn(Class, Object...)}
   * @return a {@link RhymeLinkBuilder} that you can use to decorate the link with name and title attributes, and
   *         then finally build it
   * @see WebMvcLinkBuilder
   */
  RhymeLinkBuilder create(WebMvcLinkBuilder linkBuilder) {

    return fingerprinting.createLinkWith(linkBuilder);
  }

  /**
   * An alternative signature to {@link #create(WebMvcLinkBuilder)} that allows you to specify
   * the controller class and method call directly. It's using {@link WebMvcLinkBuilder} as well,
   * so note that the function you are passing isn't called on the actual controller class,
   * but a proxy instance that does nothing else but to capture the method call and the parameters.
   * @param <T> the type of controller you want to link to
   * @param controllerClass the controller class you want to link to
   * @param handlerMethodCall a function that calls a method on a proxy of the given controller,
   *          to find the path mapping and expand any URI template variables if required
   * @return a {@link RhymeLinkBuilder} that you can use to decorate the link with name and title attributes, and
   *         then finally build it
   * @see WebMvcLinkBuilder
   */
  <T> RhymeLinkBuilder createLinkTo(Class<T> controllerClass, Function<T, LinkableResource> handlerMethodCall) {

    WebMvcLinkBuilder linkBuilder = linkTo(handlerMethodCall.apply(methodOn(controllerClass)));

    return create(linkBuilder);
  }

  /**
   * Constructs an URL to the {@link CompanyApiController}, where a {@value #TIMESTAMP_QUERY_PARAM} parameter
   * is appended only if it was also present in the incoming request. This is used by the
   * {@link DetailedEmployeeController} to ensure that it can serve both immutable and mutable versions of the resource.
   * @return a fully qualified URL for requests to the {@link CompanyApiController#get()} method
   */
  String getLocalEntryPointUrl() {

    return createLinkTo(CompanyApiController.class, CompanyApiController::get)
        .withFingerprintingOnlyIf(fingerprinting.isUsedInIncomingRequest())
        .build()
        .getHref();
  }
}