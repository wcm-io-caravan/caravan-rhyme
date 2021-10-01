package io.wcm.caravan.rhyme.spring.impl;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.Duration;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.spring.api.SpringRhyme;

/**
 * A controller handling the requests fro {@link SpringErrorHandlingIT}
 */
@RestController
public class SpringErrorHandlingController {

  public static final String BASE_PATH = "/errorHandling";

  @Autowired
  private SpringRhyme rhyme;

  @HalApiInterface
  public interface ErrorHandlingResource extends LinkableResource {

    @Related("errors:responseStatus")
    ErrorThrowingResource triggerResponseStatusException(@TemplateVariable("statusCode") Integer statusCode);

    @Related("errors:gone")
    ErrorThrowingResource triggerGoneExceptionWith100DaysMaxAge();

    @Related("errors:tooManyRequests")
    ErrorThrowingResource triggerTooManyRequests();
  }

  @HalApiInterface
  public interface ErrorThrowingResource extends LinkableResource {

    @ResourceState
    ObjectNode getStateWithError();
  }

  @ResponseStatus(HttpStatus.GONE)
  public static class GoneException extends RuntimeException {

    private static final long serialVersionUID = 1L;
  }

  @ResponseStatus(code = HttpStatus.TOO_MANY_REQUESTS)
  public static class TooManyRequestExceptions extends RuntimeException {

    private static final long serialVersionUID = 1L;
  }

  private Link createLinkTo(Function<SpringErrorHandlingController, LinkableResource> func) {
    return new Link((linkTo(func.apply(methodOn(SpringErrorHandlingController.class)))).toString());
  }

  @GetMapping(BASE_PATH)
  ErrorHandlingResource get() {

    return new ErrorHandlingResource() {

      @Override
      public ErrorThrowingResource triggerResponseStatusException(Integer statusCode) {
        return get(statusCode);
      }

      @Override
      public ErrorThrowingResource triggerGoneExceptionWith100DaysMaxAge() {
        return gone();
      }

      @Override
      public ErrorThrowingResource triggerTooManyRequests() {
        return tooManyRequests();
      }

      @Override
      public Link createLink() {
        return createLinkTo(ctrl -> ctrl.get());
      }
    };
  }

  @GetMapping(BASE_PATH + "/responseStatusException")
  ErrorThrowingResource get(@RequestParam Integer statusCode) {

    return new ErrorThrowingResource() {

      @Override
      public ObjectNode getStateWithError() {
        throw new ResponseStatusException(HttpStatus.valueOf(statusCode));
      }

      @Override
      public Link createLink() {
        return createLinkTo(ctrl -> ctrl.get(statusCode));
      }
    };
  }

  @GetMapping(BASE_PATH + "/goneException")
  ErrorThrowingResource gone() {

    return new ErrorThrowingResource() {

      @Override
      public ObjectNode getStateWithError() {

        rhyme.setResponseMaxAge(Duration.ofDays(100));

        throw new GoneException();
      }

      @Override
      public Link createLink() {
        return createLinkTo(ctrl -> ctrl.gone());
      }
    };
  }

  @GetMapping(BASE_PATH + "/tooManyRequests")
  ErrorThrowingResource tooManyRequests() {

    return new ErrorThrowingResource() {

      @Override
      public ObjectNode getStateWithError() {
        throw new TooManyRequestExceptions();
      }

      @Override
      public Link createLink() {
        return createLinkTo(ctrl -> ctrl.tooManyRequests());
      }
    };
  }
}
