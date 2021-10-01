package io.wcm.caravan.rhyme.spring.impl;

import static io.wcm.caravan.rhyme.spring.impl.SpringErrorHandlingController.BASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import io.wcm.caravan.rhyme.spring.testing.SpringRhymeIntegrationTest;
import io.wcm.caravan.rhyme.spring.testing.SpringRhymeIntegrationTestExtension;

/**
 * Using S
 */
@ExtendWith(SpringRhymeIntegrationTestExtension.class)
@SpringRhymeIntegrationTest(entryPointUri = SpringErrorHandlingIT.BASE_URI, applicationClass = SpringRhymeHypermediaApplication.class)
public class SpringErrorHandlingIT {

  static final String BASE_URI = "http://localhost:8081" + BASE_PATH;

  private final ErrorHandlingResource errors;
  private final HalApiClient client;

  SpringErrorHandlingIT(ErrorHandlingResource errors) {
    this.errors = errors;
    this.client = HalApiClient.create();
  }

  private HalResponse getResponseFromCaughtClientException(Function<ErrorHandlingResource, ErrorThrowingResource> callable) {

    HalApiClientException ex = catchThrowableOfType(() -> callable.apply(errors).getStateWithError(), HalApiClientException.class);

    assertThat(ex).isNotNull();

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

    String nonExistingUrl = BASE_URI + "/foo/bar";

    HalResponse errorResponse = getResponseFromCaughtClientException(
        (er) -> client.getRemoteResource(nonExistingUrl, ErrorThrowingResource.class));

    assertThat(errorResponse.getStatus())
        .isEqualTo(HttpStatus.NOT_FOUND.value());
  }
}
