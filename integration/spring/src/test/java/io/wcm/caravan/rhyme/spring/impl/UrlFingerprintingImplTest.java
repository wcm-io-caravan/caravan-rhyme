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

import static io.wcm.caravan.rhyme.spring.impl.SpringLinkBuilderTestController.BASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.spring.api.UrlFingerprinting;


@ExtendWith(MockitoExtension.class)
public class UrlFingerprintingImplTest {

  private static final String PARAM = "time";
  private static final String PARAM2 = "time2";

  private static final Duration MUTABLE_MAX_AGE = Duration.ofSeconds(1);
  private static final Duration IMMUTABLE_MAX_AGE = Duration.ofSeconds(100);

  private final WebMvcLinkBuilder linkBuilder = linkTo(methodOn(SpringLinkBuilderTestController.class).get());

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

  ResponseEntity<String> controllerWithRequiredAnnotatedParamFoo(@RequestParam("foo") String foo) {
    return ResponseEntity.ok("foo was set to " + foo);
  }

  @Test
  public void should_expand_value_of_required_annotated_parameter() throws Exception {

    UrlFingerprinting fingerprinting = createFingerprinting();

    Link link = fingerprinting.createLinkWith(linkTo(methodOn(UrlFingerprintingImplTest.class)
        .controllerWithRequiredAnnotatedParamFoo("test")))
        .build();

    assertThat(link.getHref())
        .endsWith("?foo=test");
  }

  @Test
  public void should_create_template_for_required_annotated_parameter() throws Exception {

    UrlFingerprinting fingerprinting = createFingerprinting();

    Link link = fingerprinting.createLinkWith(linkTo(methodOn(UrlFingerprintingImplTest.class)
        .controllerWithRequiredAnnotatedParamFoo(null)))
        .build();

    assertThat(link.isTemplated());
    assertThat(link.getHref())
        .endsWith("?foo={foo}");
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

    verifyZeroInteractions(rhyme);
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

  @Test
  public void should_add_additional_query_parameters() throws Exception {

    UrlFingerprinting fingerprinting = createFingerprinting()
        .withQueryParameter("foo", "123")
        .withQueryParameter("bar", "456");

    Link link = createLinkWith(fingerprinting);

    assertThat(link.getHref())
        .contains("foo=123&bar=456");
  }

  @Test
  public void additional_query_parameters_should_not_replace_existing() throws Exception {

    UrlFingerprinting fingerprinting = createFingerprinting()
        .withQueryParameter("foo", "123")
        .withQueryParameter("bar", "456");

    Link link = fingerprinting.createLinkWith(linkTo(methodOn(UrlFingerprintingImplTest.class)
        .controllerWithRequiredAnnotatedParamFoo("test")))
        .build();

    assertThat(link.getHref())
        .contains("foo=test&bar=456");
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
