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
package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.Iterables;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.spring.testing.HalCrawler;
import io.wcm.caravan.rhyme.spring.testing.MockMvcHalResourceLoaderConfiguration;

@SpringBootTest
@Import(MockMvcHalResourceLoaderConfiguration.class)
public class UrlFingerprintingIT {

  private static final int SHORT_MAX_AGE_SECONDS = 10;
  private static final int LONG_MAX_AGE_SECONDS = (int)Duration.ofDays(100).getSeconds();

  @Autowired
  private HalResourceLoader mockMvcResourceLoader;

  private HalResponse getResponse(String uri) {

    return mockMvcResourceLoader.getHalResource(uri).blockingGet();
  }

  private List<String> getAllLinkHrefsFrom(HalResponse entryPoint) {

    return entryPoint.getBody().getLinks().entries().stream()
        .filter(entry -> !entry.getKey().equals("curies"))
        .map(Entry::getValue)
        .map(Link::getHref)
        .collect(Collectors.toList());
  }

  private String getTimestampQueryParamFromUri(String uri) {

    return UriComponentsBuilder.fromUriString(uri)
        .build()
        .getQueryParams()
        .getFirst("timestamp");
  }

  @Test
  void max_age_of_entry_point_without_timestamp_should_be_short() {

    Integer maxAge = getResponse("/").getMaxAge();

    assertThat(maxAge)
        .isEqualTo(SHORT_MAX_AGE_SECONDS);
  }

  @Test
  void max_age_of_entry_point_with_timestamp_should_be_long() {

    Integer maxAge = getResponse("/?timestamp=foo").getMaxAge();

    assertThat(maxAge)
        .isEqualTo(LONG_MAX_AGE_SECONDS);
  }

  @Test
  void entry_point_should_add_current_timestamp_query_to_all_links() {

    HalResponse entryPoint = getResponse("/");

    List<String> linkUris = getAllLinkHrefsFrom(entryPoint);

    assertThat(linkUris)
        .extracting(this::getTimestampQueryParamFromUri)
        .doesNotContainNull();
  }

  @Test
  void entry_point_should_add_incoming_timestamp_query_to_all_links() {

    HalResponse entryPoint = getResponse("/?timestamp=foo");

    List<String> linkUris = getAllLinkHrefsFrom(entryPoint);

    assertThat(linkUris)
        .extracting(this::getTimestampQueryParamFromUri)
        .containsOnly("foo");
  }

  @Test
  void all_other_resources_should_have_long_max_age_and_timestamp() {

    HalCrawler crawler = new HalCrawler(mockMvcResourceLoader);

    List<HalResponse> responses = crawler.getAllResponses();

    Iterable<HalResponse> responsesWithoutEntrypoint = Iterables.skip(responses, 1);

    assertThat(responsesWithoutEntrypoint)
        .extracting(HalResponse::getMaxAge)
        .containsOnly(LONG_MAX_AGE_SECONDS);

    assertThat(responsesWithoutEntrypoint)
        .extracting(HalResponse::getUri)
        .extracting(this::getTimestampQueryParamFromUri)
        .doesNotContainNull();
  }

  @Test
  void all_resources_should_have_short_max_age_if_called_without_query() {

    HalCrawler crawlerThatRemovesQuery = new HalCrawler(mockMvcResourceLoader)
        .withModifiedUrls(this::removeQuery);

    List<HalResponse> responses = crawlerThatRemovesQuery.getAllResponses();

    assertThat(responses)
        .extracting(HalResponse::getMaxAge)
        .containsOnly(SHORT_MAX_AGE_SECONDS);
  }

  private String removeQuery(String url) {

    return UriComponentsBuilder.fromUriString(url)
        .replaceQuery(null)
        .build()
        .toUriString();
  }
}
