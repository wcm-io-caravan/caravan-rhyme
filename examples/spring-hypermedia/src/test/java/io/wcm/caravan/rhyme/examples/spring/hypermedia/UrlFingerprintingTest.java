package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.UriComponentsBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.spring.testing.HalCrawler;
import io.wcm.caravan.rhyme.spring.testing.MockMvcHalResourceLoader;
import wiremock.com.google.common.collect.Iterables;

@SpringBootTest
public class UrlFingerprintingTest {

  private static final int SHORT_MAX_AGE_SECONDS = 10;
  private static final int LONG_MAX_AGE_SECONDS = (int)Duration.ofDays(100).getSeconds();

  @Autowired
  private MockMvcHalResourceLoader mockMvcResourceLoader;

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
  void max_age_of_entry_point_without_timestamp_should_be_long() {

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
        .withModifiedUrls(uri -> uri.replaceQuery(null));

    List<HalResponse> responses = crawlerThatRemovesQuery.getAllResponses();

    assertThat(responses)
        .extracting(HalResponse::getMaxAge)
        .containsOnly(SHORT_MAX_AGE_SECONDS);
  }

}
