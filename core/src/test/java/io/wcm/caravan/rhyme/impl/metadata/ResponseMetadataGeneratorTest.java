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
package io.wcm.caravan.rhyme.impl.metadata;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.VIA;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.EMISSION_TIMES;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.MAX_AGE;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.RENDERING_TIMES;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.RESPONSE_TIMES;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.SOURCE_LINKS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.impl.metadata.FullMetadataGenerator.TimeMeasurement;

@ExtendWith(MockitoExtension.class)
class ResponseMetadataGeneratorTest {

  private static final String METHOD2 = "method2()";
  private static final String METHOD1 = "method1()";
  private static final String UPSTREAM_URI1 = "/1";
  private static final String UPSTREAM_URI2 = "/2";
  private static final String UPSTREAM_URI3 = "/2";
  private static final String UPSTREAM_TITLE = "title";

  private static final int ANY_RESPONSE_TIME = 400;

  @Mock
  private LinkableResource resource;

  private final FullMetadataGenerator metrics = new FullMetadataGenerator();

  @Test
  void default_output_max_age_should_be_null() {

    assertThat(metrics.getResponseMaxAge()).isNull();
  }

  @Test
  void output_max_age_should_be_null_if_no_max_age_present_in_upstream_response() {

    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, null, ANY_RESPONSE_TIME);

    assertThat(metrics.getResponseMaxAge()).isNull();
  }

  @Test
  void max_age_limit_should_handle_durations_longer_than_max_int() {

    metrics.setResponseMaxAge(Duration.ofDays(Integer.MAX_VALUE));

    assertThat(metrics.getResponseMaxAge()).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void max_age_limit_should_be_used_if_no_resources_are_requested() {

    int limit = 55;
    metrics.setResponseMaxAge(Duration.ofSeconds(limit));

    assertThat(metrics.getResponseMaxAge()).isEqualTo(limit);
  }

  @Test
  void max_age_limit_should_use_lowest_value_of_multiple_calls() {

    int lowerLimit = 55;
    metrics.setResponseMaxAge(Duration.ofSeconds(lowerLimit));

    metrics.setResponseMaxAge(Duration.ofSeconds(123));

    assertThat(metrics.getResponseMaxAge()).isEqualTo(lowerLimit);
  }

  @Test
  void max_age_limit_should_use_lowest_value_of_multiple_calls_2() {

    int lowerLimit = 55;

    metrics.setResponseMaxAge(Duration.ofSeconds(123));

    metrics.setResponseMaxAge(Duration.ofSeconds(lowerLimit));

    assertThat(metrics.getResponseMaxAge()).isEqualTo(lowerLimit);
  }

  @Test
  void max_age_limit_should_be_used_if_if_no_max_age_present_in_upstream_response() {

    int limit = 55;
    metrics.setResponseMaxAge(Duration.ofSeconds(limit));
    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, null, ANY_RESPONSE_TIME);

    assertThat(metrics.getResponseMaxAge()).isEqualTo(limit);
  }

  @Test
  void max_age_limit_should_be_used_if_its_smaller_than_max_age_from_upstream_response() {

    int limit = 55;
    metrics.setResponseMaxAge(Duration.ofSeconds(limit));
    int upstreamMaxAge = 400;
    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, upstreamMaxAge, ANY_RESPONSE_TIME);

    assertThat(metrics.getResponseMaxAge()).isEqualTo(limit);
  }

  @Test
  void max_age_from_upstream_resource_should_be_used_if_no_limit_is_set() {

    int upstreamMaxAge = 40;
    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, upstreamMaxAge, ANY_RESPONSE_TIME);

    assertThat(metrics.getResponseMaxAge()).isEqualTo(upstreamMaxAge);
  }

  @Test
  void max_age_from_upstream_resource_should_be_used_if_its_smaller_than_limit() {

    int limit = 55;
    metrics.setResponseMaxAge(Duration.ofSeconds(limit));
    int upstreamMaxAge = 40;
    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, upstreamMaxAge, ANY_RESPONSE_TIME);

    assertThat(metrics.getResponseMaxAge()).isEqualTo(upstreamMaxAge);
  }

  @Test
  void smallest_max_age_from_upstream_resources_should_be_used_if_no_limit_is_set() {

    int upstreamMaxAge1 = 400;
    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, upstreamMaxAge1, ANY_RESPONSE_TIME);
    int upstreamMaxAge2 = 40;
    metrics.onResponseRetrieved(UPSTREAM_URI2, UPSTREAM_TITLE, upstreamMaxAge2, ANY_RESPONSE_TIME);

    assertThat(metrics.getResponseMaxAge()).isEqualTo(upstreamMaxAge2);
  }

  @Test
  void max_age_times_from_responses_should_be_collected_and_ordered() {

    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, 10, ANY_RESPONSE_TIME);
    metrics.onResponseRetrieved(UPSTREAM_URI2, UPSTREAM_TITLE, 200, ANY_RESPONSE_TIME);
    metrics.onResponseRetrieved(UPSTREAM_URI3, UPSTREAM_TITLE, 30, ANY_RESPONSE_TIME);

    List<TimeMeasurement> maxAge = metrics.getSortedInputMaxAgeSeconds();

    assertThat(maxAge).hasSize(3);
    assertThat(maxAge.get(0).getUnit()).isEqualTo(TimeUnit.SECONDS);
    assertThat(maxAge.get(0).getTime()).isEqualTo(200.f);
    assertThat(maxAge.get(1).getTime()).isEqualTo(30.f);
    assertThat(maxAge.get(2).getTime()).isEqualTo(10.f);
  }

  @Test
  void response_times_from_upstream_should_be_collected_and_ordered() {

    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, null, 10);
    metrics.onResponseRetrieved(UPSTREAM_URI2, UPSTREAM_TITLE, null, 200);
    metrics.onResponseRetrieved(UPSTREAM_URI3, UPSTREAM_TITLE, null, 30);

    List<TimeMeasurement> maxAge = metrics.getSortedInputResponseTimes();

    assertThat(maxAge).hasSize(3);
    assertThat(maxAge.get(0).getUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    assertThat(maxAge.get(0).getTime()).isEqualTo(0.2f);
    assertThat(maxAge.get(1).getTime()).isEqualTo(0.03f);
    assertThat(maxAge.get(2).getTime()).isEqualTo(0.01f);
  }

  @Test
  void upstream_source_links_should_be_collected_in_original_order() {

    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, null, 10);
    metrics.onResponseRetrieved(UPSTREAM_URI2, UPSTREAM_TITLE, null, 200);
    metrics.onResponseRetrieved(UPSTREAM_URI3, UPSTREAM_TITLE, null, 30);

    List<Link> links = metrics.getSourceLinks();

    assertThat(links).hasSize(3);
    assertThat(links.get(0).getHref()).isEqualTo(UPSTREAM_URI1);
    assertThat(links.get(1).getHref()).isEqualTo(UPSTREAM_URI2);
    assertThat(links.get(2).getHref()).isEqualTo(UPSTREAM_URI3);
  }

  @Test
  void emission_times_should_be_grouped_by_method_and_ordered_by_max() {

    metrics.onMethodInvocationFinished(HalApiClient.class, METHOD1, 1500);
    metrics.onMethodInvocationFinished(HalApiClient.class, METHOD1, 500);
    metrics.onMethodInvocationFinished(HalApiClient.class, METHOD2, 2000);

    List<TimeMeasurement> maxAge = metrics.getGroupedAndSortedInvocationTimes(HalApiClient.class.getSimpleName(), true);

    assertThat(maxAge).hasSize(2);
    assertThat(maxAge.get(0).getUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    assertThat(maxAge.get(0).getText()).isEqualTo("1x " + METHOD2);
    assertThat(maxAge.get(0).getTime()).isCloseTo(2.0f, withPercentage(0.01f));
    assertThat(maxAge.get(1).getUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    assertThat(maxAge.get(1).getText()).isEqualTo("max of 2x " + METHOD1);
    assertThat(maxAge.get(1).getTime()).isCloseTo(1.5f, withPercentage(0.01f));
  }

  @Test
  void emission_times_should_be_grouped_by_method_and_ordered_by_sum() {

    metrics.onMethodInvocationFinished(HalApiClient.class, METHOD1, 600);
    metrics.onMethodInvocationFinished(HalApiClient.class, METHOD1, 800);
    metrics.onMethodInvocationFinished(HalApiClient.class, METHOD2, 1000);

    List<TimeMeasurement> maxAge = metrics.getGroupedAndSortedInvocationTimes(HalApiClient.class.getSimpleName(), false);

    assertThat(maxAge).hasSize(2);
    assertThat(maxAge.get(0).getUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    assertThat(maxAge.get(0).getText()).isEqualTo("sum of 2x " + METHOD1);
    assertThat(maxAge.get(0).getTime()).isCloseTo(1.4f, withPercentage(0.01f));
    assertThat(maxAge.get(1).getUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    assertThat(maxAge.get(1).getText()).isEqualTo("1x " + METHOD2);
    assertThat(maxAge.get(1).getTime()).isCloseTo(1.f, withPercentage(0.01f));
  }

  @Test
  void metadata_resource_can_be_created_without_interactions() {

    HalResource metadata = metrics.createMetadataResource(resource);

    assertThat(metadata).isNotNull();
  }

  @Test
  void metadata_resource_can_be_created_with_null_resource_instance() {

    HalResource metadata = metrics.createMetadataResource(null);

    assertThat(metadata).isNotNull();
  }

  @Test
  void metadata_resource_contains_source_links() {

    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, 10, ANY_RESPONSE_TIME);
    metrics.onResponseRetrieved(UPSTREAM_URI2, UPSTREAM_TITLE, 200, ANY_RESPONSE_TIME);
    metrics.onResponseRetrieved(UPSTREAM_URI3, UPSTREAM_TITLE, 30, ANY_RESPONSE_TIME);

    HalResource metadata = metrics.createMetadataResource(resource);

    List<Link> sourceLinks = metadata.getEmbeddedResource(SOURCE_LINKS).getLinks(VIA);
    assertThat(sourceLinks).hasSize(3);
    assertThat(sourceLinks.get(0).getHref()).isEqualTo(UPSTREAM_URI1);
    assertThat(sourceLinks.get(0).getTitle()).isEqualTo(UPSTREAM_TITLE);
    assertThat(sourceLinks.get(1).getHref()).isEqualTo(UPSTREAM_URI3);
    assertThat(sourceLinks.get(1).getTitle()).isEqualTo(UPSTREAM_TITLE);
    assertThat(sourceLinks.get(2).getHref()).isEqualTo(UPSTREAM_URI2);
    assertThat(sourceLinks.get(2).getTitle()).isEqualTo(UPSTREAM_TITLE);

  }

  @Test
  void metadata_resource_contains_embedded_details_metrics_if_timings_were_collected() {

    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, 10, ANY_RESPONSE_TIME);
    metrics.onMethodInvocationFinished(EmissionStopwatch.class, METHOD1, ANY_RESPONSE_TIME);
    metrics.onMethodInvocationFinished(AsyncHalResponseRenderer.class, METHOD1, ANY_RESPONSE_TIME);

    HalResource metadata = metrics.createMetadataResource(resource);

    assertThat(metadata.hasEmbedded(RESPONSE_TIMES)).isTrue();
    assertThat(metadata.hasEmbedded(EMISSION_TIMES)).isTrue();
    assertThat(metadata.hasEmbedded(RENDERING_TIMES)).isTrue();
    assertThat(metadata.hasEmbedded(MAX_AGE)).isTrue();
  }

  @Test
  void metadata_resource_contains_no_embedded_details_metrics_if_no_timings_were_collected() {

    HalResource metadata = metrics.createMetadataResource(resource);

    assertThat(metadata.hasEmbedded(RESPONSE_TIMES)).isFalse();
    assertThat(metadata.hasEmbedded(EMISSION_TIMES)).isFalse();
    assertThat(metadata.hasEmbedded(RENDERING_TIMES)).isFalse();
    assertThat(metadata.hasEmbedded(MAX_AGE)).isFalse();
  }

  @Test
  void metadata_resource_contains_sum_of_invocation_and_response_times() {

    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, null, 2000);
    metrics.onResponseRetrieved(UPSTREAM_URI2, UPSTREAM_TITLE, null, 3000);

    metrics.onMethodInvocationFinished(HalApiClient.class, METHOD1, 200);
    metrics.onMethodInvocationFinished(HalApiClient.class, METHOD1, 300);

    metrics.onMethodInvocationFinished(AsyncHalResponseRenderer.class, METHOD1, 100);
    metrics.onMethodInvocationFinished(AsyncHalResponseRenderer.class, METHOD1, 200);

    HalResource metadata = metrics.createMetadataResource(resource);

    assertThat(metadata.getModel().path("sumOfResponseAndParseTimes").asText()).isEqualTo("5.0ms");
    assertThat(metadata.getModel().path("sumOfProxyInvocationTime").asText()).isEqualTo("0.5ms");
    assertThat(metadata.getModel().path("sumOfResourceAssemblyTime").asText()).isEqualTo("0.3ms");
  }

  @Test
  void responses_that_were_retrieved_after_metadata_generation_should_be_ignored() {

    HalResource metadata = metrics.createMetadataResource(resource);

    metrics.onResponseRetrieved(UPSTREAM_URI1, UPSTREAM_TITLE, null, 100);

    assertThat(metadata.getModel().path("sumOfResponseAndParseTimes").asText()).isEqualTo("0.0ms");
  }
}
