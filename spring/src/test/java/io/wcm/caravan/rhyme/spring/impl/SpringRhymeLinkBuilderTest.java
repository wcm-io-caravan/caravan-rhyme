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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import io.wcm.caravan.hal.resource.Link;
import wiremock.com.google.common.collect.ImmutableMap;


public class SpringRhymeLinkBuilderTest {

  private static final String EXPECTED_URL = "/errorHandling/responseStatusException?statusCode=200";
  private static final String EXPECTED_TEMPLATE = "/errorHandling/responseStatusException?statusCode={statusCode}";

  private SpringRhymeLinkBuilder createLinkBuilder(Integer statusCode, Map<String, String> timestampParameters) {

    WebMvcLinkBuilder linkBuilder = linkTo(methodOn(SpringErrorHandlingController.class).responseStatus(statusCode));

    return new SpringRhymeLinkBuilder(linkBuilder, timestampParameters);
  }

  private SpringRhymeLinkBuilder buildResolvedUri() {

    return createLinkBuilder(200, Collections.emptyMap());
  }

  private SpringRhymeLinkBuilder buildUriTemplate() {

    return createLinkBuilder(null, Collections.emptyMap());
  }

  private SpringRhymeLinkBuilder buildResolvedUriWithFingerprinting(Map<String, String> timestampParameters) {

    return createLinkBuilder(200, timestampParameters);
  }

  private SpringRhymeLinkBuilder buildUriTemplateWithFingerprinting(Map<String, String> timestampParameters) {

    return createLinkBuilder(null, timestampParameters);
  }


  @Test
  public void build_can_be_called_immediately() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildResolvedUri();

    Link link = linkBuilder.build();

    assertThat(link.getHref()).isEqualTo(EXPECTED_URL);
    assertThat(link.getTitle()).isNull();
    assertThat(link.getName()).isNull();
  }

  @Test
  public void build_can_be_called_immediately_for_template() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildUriTemplate();

    Link link = linkBuilder.build();

    assertThat(link.getHref()).isEqualTo(EXPECTED_TEMPLATE);
    assertThat(link.getTitle()).isNull();
    assertThat(link.getName()).isNull();
  }

  @Test
  public void build_adds_timestamp_parameters_by_default() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildResolvedUriWithFingerprinting(ImmutableMap.of("foo", "bar"));

    Link link = linkBuilder.build();

    assertThat(link.getHref()).isEqualTo(EXPECTED_URL + "&foo=bar");
  }

  @Test
  public void build_adds_timestamp_parameters_to_template() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildUriTemplateWithFingerprinting(ImmutableMap.of("foo", "bar"));

    Link link = linkBuilder.build();

    assertThat(link.getHref()).isEqualTo(EXPECTED_TEMPLATE + "&foo=bar");
  }

  @Test
  public void withFingerprintingOnlyIf_should_keep_timestamp_if_condition_is_true() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildResolvedUriWithFingerprinting(ImmutableMap.of("foo", "bar"));

    Link link = linkBuilder.withFingerprintingOnlyIf(true).build();

    assertThat(link.getHref()).isEqualTo(EXPECTED_URL + "&foo=bar");
  }

  @Test
  public void withFingerprintingOnlyIf_should_remove_timestamp_if_condition_is_false() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildResolvedUriWithFingerprinting(ImmutableMap.of("foo", "bar"));

    Link link = linkBuilder.withFingerprintingOnlyIf(false).build();

    assertThat(link.getHref()).isEqualTo(EXPECTED_URL);
  }

  @Test
  public void withName_should_add_name_attribute() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildResolvedUri();

    Link link = linkBuilder.withName("foo").build();

    assertThat(link.getName()).isEqualTo("foo");
  }

  @Test
  public void withTitle_should_add_title_attribute() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildResolvedUri();

    Link link = linkBuilder.withTitle("foo").build();

    assertThat(link.getTitle()).isEqualTo("foo");
  }

  @Test
  public void withTitle_should_add_title_attribute_also_if_link_is_templated() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildUriTemplate();

    Link link = linkBuilder.withTitle("foo").build();

    assertThat(link.isTemplated()).isTrue();
    assertThat(link.getTitle()).isEqualTo("foo");
  }

  @Test
  public void withTemplateTitle_should_add_title_attribute_if_link_is_template() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildUriTemplate();

    Link link = linkBuilder.withTemplateTitle("foo").build();

    assertThat(link.isTemplated()).isTrue();
    assertThat(link.getTitle()).isEqualTo("foo");
  }

  @Test
  public void withTemplateTitle_should_not_add_title_attribute_if_link_is_resolved() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildResolvedUri();

    Link link = linkBuilder.withTemplateTitle("foo").build();

    assertThat(link.isTemplated()).isFalse();
    assertThat(link.getTitle()).isNull();
  }

  @Test
  public void withTemplateTitle_can_be_called_before_withTitle() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildUriTemplate();

    Link link = linkBuilder
        .withTemplateTitle("template title")
        .withTitle("resolved title")
        .build();

    assertThat(link.isTemplated()).isTrue();
    assertThat(link.getTitle()).isEqualTo("template title");
  }

  @Test
  public void withTemplateTitle_can_be_called_after_withTitle() throws Exception {

    SpringRhymeLinkBuilder linkBuilder = buildUriTemplate();

    Link link = linkBuilder
        .withTitle("resolved title")
        .withTemplateTitle("template title")
        .build();

    assertThat(link.isTemplated()).isTrue();
    assertThat(link.getTitle()).isEqualTo("template title");
  }
}
