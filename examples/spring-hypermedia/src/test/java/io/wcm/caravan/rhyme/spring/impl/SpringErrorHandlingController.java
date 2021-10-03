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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.function.Function;

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

/**
 * A controller handling the requests from {@link SpringErrorHandlingIT}
 */
@RestController
public class SpringErrorHandlingController {

  public static final String BASE_PATH = "/errorHandling";

  @HalApiInterface
  public interface ErrorHandlingResource extends LinkableResource {

    @Related("errors:responseStatus")
    ErrorThrowingResource triggerResponseStatusException(@TemplateVariable("statusCode") Integer statusCode);

    @Related("errors:gone")
    ErrorThrowingResource triggerGoneException();

    @Related("errors:tooManyRequests")
    ErrorThrowingResource triggerTooManyRequests();
  }

  @HalApiInterface
  public interface ErrorThrowingResource extends LinkableResource {

    @ResourceState
    ObjectNode getStateWithError();
  }

  @ResponseStatus(HttpStatus.GONE)
  private static class ExceptionWithGoneStatusInValueAttribute extends RuntimeException {

    private static final long serialVersionUID = 1L;
  }

  @ResponseStatus(code = HttpStatus.TOO_MANY_REQUESTS)
  private static class ExceptionWithTooManyRequestsStatusInCodeAttribute extends RuntimeException {

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
        return responseStatus(statusCode);
      }

      @Override
      public ErrorThrowingResource triggerGoneException() {
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
  ErrorThrowingResource responseStatus(@RequestParam Integer statusCode) {

    return new ErrorThrowingResource() {

      @Override
      public ObjectNode getStateWithError() {
        throw new ResponseStatusException(HttpStatus.valueOf(statusCode));
      }

      @Override
      public Link createLink() {
        return createLinkTo(ctrl -> ctrl.responseStatus(statusCode));
      }
    };
  }

  @GetMapping(BASE_PATH + "/goneException")
  ErrorThrowingResource gone() {

    return new ErrorThrowingResource() {

      @Override
      public ObjectNode getStateWithError() {
        throw new ExceptionWithGoneStatusInValueAttribute();
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
        throw new ExceptionWithTooManyRequestsStatusInCodeAttribute();
      }

      @Override
      public Link createLink() {
        return createLinkTo(ctrl -> ctrl.tooManyRequests());
      }
    };
  }
}
