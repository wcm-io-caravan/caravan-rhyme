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

import static io.wcm.caravan.rhyme.spring.impl.SpringErrorHandlingController.BASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.HttpStatus;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.examples.spring.hypermedia.SpringRhymeHypermediaApplication;
import io.wcm.caravan.rhyme.spring.impl.SpringErrorHandlingController.ErrorHandlingResource;
import io.wcm.caravan.rhyme.spring.impl.SpringErrorHandlingController.ErrorThrowingResource;


/**
 * An integration test for the error handling in {@link VndErrorHandlingControllerAdvice} and
 * {@link SpringExceptionStatusAndLoggingStrategy}.
 * It's using {@link WebEnvironment#RANDOM_PORT} to fully start up the
 * {@link SpringRhymeHypermediaApplication} listening on a random port, because
 * there are a few error cases in this test where {@link WebEnvironment#MOCK} doesn't work the same way
 * as if the application is serving requests on an actual network socket.
 */
@SpringBootTest(classes = SpringRhymeHypermediaApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class SpringErrorHandlingIT {

  private final String baseUri;


  private final ErrorHandlingResource errors;
  private final HalApiClient client;

  SpringErrorHandlingIT(@Autowired ServletWebServerApplicationContext server) {

    this.baseUri = "http://localhost:" + server.getWebServer().getPort() + BASE_PATH;

    this.client = HalApiClient.create();
    this.errors = client.getRemoteResource(baseUri, ErrorHandlingResource.class);
  }

  private HalResponse getResponseFromCaughtClientException(Function<ErrorHandlingResource, ErrorThrowingResource> callable) {

    ErrorThrowingResource errorResource = callable.apply(errors);

    HalApiClientException ex = assertThrows(HalApiClientException.class, errorResource::getStateWithError);

    assertThat(ex.getErrorResponse()).isNotNull();
    assertThat(ex.getErrorResponse().getContentType()).isEqualTo(VndErrorResponseRenderer.CONTENT_TYPE);

    return ex.getErrorResponse();
  }

  @Test
  void should_extract_status_code_from_ResponseStatusException() {

    int status = 505;

    HalResponse errorResponse = getResponseFromCaughtClientException(
        (er) -> er.triggerResponseStatusException(status));

    assertThat(errorResponse.getStatus())
        .isEqualTo(status);
  }

  @Test
  void should_extract_status_code_from_ResponseStatus_annotation() {

    HalResponse errorResponse = getResponseFromCaughtClientException(
        (er) -> er.triggerGoneException());

    assertThat(errorResponse.getStatus())
        .isEqualTo(HttpStatus.GONE.value());
  }

  @Test
  void should_extract_status_code_from_ResponseStatus_annotation_code() {

    HalResponse errorResponse = getResponseFromCaughtClientException(
        (er) -> er.triggerTooManyRequests());

    assertThat(errorResponse.getStatus())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
  }

  @Test
  void should_extract_status_code_from_MissingServletRequestParameterException() {

    Link link = errors.triggerResponseStatusException(404).createLink();

    String urlWithoutQuery = StringUtils.substringBefore(link.getHref(), "?");

    HalResponse errorResponse = getResponseFromCaughtClientException(
        (er) -> client.getRemoteResource(urlWithoutQuery, ErrorThrowingResource.class));

    assertThat(errorResponse.getStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  void should_extract_status_code_from_MethodArgumentTypeMismatchException() {

    Link link = errors.triggerResponseStatusException(null).createLink();

    String urlWithInvalidQuery = UriTemplate.fromTemplate(link.getHref())
        .expand(ImmutableMap.of("statusCode", "foo"));

    HalResponse errorResponse = getResponseFromCaughtClientException(
        (er) -> client.getRemoteResource(urlWithInvalidQuery, ErrorThrowingResource.class));

    assertThat(errorResponse.getStatus())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  void should_extract_status_code_from_NoHandlerFoundException() {

    String nonExistingUrl = baseUri + "/foo/bar";

    HalResponse errorResponse = getResponseFromCaughtClientException(
        (er) -> client.getRemoteResource(nonExistingUrl, ErrorThrowingResource.class));

    assertThat(errorResponse.getStatus())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
  }
}
