package io.wcm.caravan.rhyme.spring.impl;

import static io.wcm.caravan.rhyme.spring.impl.SpringErrorHandlingController.BASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.spring.api.UrlFingerprinting;


@ExtendWith(MockitoExtension.class)
public class UrlFingerprintingImplTest {

  private static final String PARAM = "time";
  private static final String PARAM2 = "time2";

  private static final Duration MUTABLE_MAX_AGE = Duration.ofSeconds(1);
  private static final Duration IMMUTABLE_MAX_AGE = Duration.ofSeconds(100);

  private final WebMvcLinkBuilder linkBuilder = linkTo(methodOn(SpringErrorHandlingController.class).get());

  private static final Instant NOW = Instant.now();

  @Mock
  private Rhyme rhyme;

  @Mock
  private HttpServletRequest request;

  private UrlFingerprinting createFingerprinting() {

    return new UrlFingerprintingImpl(request, rhyme);
  }

  @Test
  public void should_build_links_if_no_other_methods_are_called() throws Exception {

    UrlFingerprinting fingerprinting = createFingerprinting();

    assertThatNoFingerprintingIsPresentInLink(fingerprinting);
  }

  @Test
  public void should_use_timestamp_from_request_if_present() throws Exception {

    String valueFromRequest = "123";

    when(request.getParameter(PARAM)).thenReturn(valueFromRequest);

    UrlFingerprinting fingerprinting = createFingerprinting()
        .withTimestampParameter(PARAM, () -> NOW);

    assertThat(fingerprinting.isUsedInIncomingRequest()).isTrue();
    assertThatFingerprintContains(fingerprinting, valueFromRequest);
  }

  @Test
  public void should_use_timestamp_from_supplier_if_not_present_in_request() throws Exception {

    UrlFingerprinting fingerprinting = createFingerprinting()
        .withTimestampParameter(PARAM, () -> NOW);

    assertThat(fingerprinting.isUsedInIncomingRequest()).isFalse();
    assertThatFingerprintContains(fingerprinting, NOW.toString());
  }

  @Test
  public void should_handle_multiple_params_present_in_request() throws Exception {

    String valueFromRequest = "123";
    String valueFromRequest2 = "123";

    when(request.getParameter(PARAM)).thenReturn(valueFromRequest);
    when(request.getParameter(PARAM2)).thenReturn(valueFromRequest2);

    UrlFingerprinting fingerprinting = createFingerprinting()
        .withTimestampParameter(PARAM, () -> NOW)
        .withTimestampParameter(PARAM2, () -> NOW);

    assertThat(fingerprinting.isUsedInIncomingRequest()).isTrue();
    assertThatFingerprintContains(fingerprinting, valueFromRequest, valueFromRequest2);
  }

  @Test
  public void should_handle_multiple_params_not_all_of_which_are_present_in_request() throws Exception {

    String valueFromRequest = "123";

    when(request.getParameter(PARAM)).thenReturn(valueFromRequest);

    UrlFingerprinting fingerprinting = createFingerprinting()
        .withTimestampParameter(PARAM, () -> NOW)
        .withTimestampParameter(PARAM2, () -> NOW);

    assertThat(fingerprinting.isUsedInIncomingRequest()).isFalse();
    assertThatFingerprintContains(fingerprinting, valueFromRequest, NOW.toString());
  }

  @Test
  public void should_not_set_max_age_if_withConditionalMaxAge_was_not_called() {

    String valueFromRequest = "123";
    when(request.getParameter(PARAM)).thenReturn(valueFromRequest);

    UrlFingerprinting fingerprinting = createFingerprinting()
        .withTimestampParameter(PARAM, () -> NOW);

    assertThatFingerprintContains(fingerprinting, valueFromRequest);

    verifyNoInteractions(rhyme);
  }

  @Test
  public void should_set_mutable_max_age_if_timestamp_not_present_in_request() {

    UrlFingerprinting fingerprinting = createFingerprinting()
        .withTimestampParameter(PARAM, () -> NOW)
        .withConditionalMaxAge(MUTABLE_MAX_AGE, IMMUTABLE_MAX_AGE);

    assertThatFingerprintContains(fingerprinting, NOW.toString());

    verify(rhyme).setResponseMaxAge(MUTABLE_MAX_AGE);
  }

  @Test
  public void should_set_immutable_max_age_if_timestamp_present_in_request() {

    String valueFromRequest = "123";
    when(request.getParameter(PARAM)).thenReturn(valueFromRequest);

    UrlFingerprinting fingerprinting = createFingerprinting()
        .withTimestampParameter(PARAM, () -> NOW)
        .withConditionalMaxAge(MUTABLE_MAX_AGE, IMMUTABLE_MAX_AGE);

    assertThatFingerprintContains(fingerprinting, valueFromRequest);

    verify(rhyme).setResponseMaxAge(IMMUTABLE_MAX_AGE);
  }

  private void assertThatNoFingerprintingIsPresentInLink(UrlFingerprinting fingerprinting) {

    Link link = createLinkWith(fingerprinting);

    assertThat(link.getHref())
        .isEqualTo(BASE_PATH);
  }

  private void assertThatFingerprintContains(UrlFingerprinting fingerprinting, String expectedValue) {

    Link link = createLinkWith(fingerprinting);

    assertThat(link.getHref())
        .isEqualTo(BASE_PATH + "?time=" + expectedValue);
  }

  private void assertThatFingerprintContains(UrlFingerprinting fingerprinting, String expectedValue1, String expectedValue2) {

    Link link = createLinkWith(fingerprinting);

    assertThat(link.getHref())
        .isEqualTo(BASE_PATH + "?time=" + expectedValue1 + "&time2=" + expectedValue2);
  }

  private Link createLinkWith(UrlFingerprinting fingerprinting) {

    return fingerprinting.createLinkWith(linkBuilder).build();
  }
}
