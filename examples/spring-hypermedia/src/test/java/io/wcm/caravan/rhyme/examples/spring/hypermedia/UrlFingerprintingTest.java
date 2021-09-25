package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.spring.testing.MockMvcHalResourceLoader;

@SpringBootTest
public class UrlFingerprintingTest {

  @Autowired
  private MockMvcHalResourceLoader mockMvcResourceLoader;


  private HalResponse getResponse(String uri) {

    return mockMvcResourceLoader.getHalResource(uri).blockingGet();
  }

  private List<UriComponents> getAllLinksAsUriComponents(HalResponse entryPoint) {

    return entryPoint.getBody().getLinks().entries().stream()
        .filter(entry -> !entry.getKey().equals("curies"))
        .map(Entry::getValue)
        .map(Link::getHref)
        .map(UriComponentsBuilder::fromUriString)
        .map(UriComponentsBuilder::build)
        .collect(Collectors.toList());
  }

  @Test
  void max_age_of_entry_point_without_timestamp_should_be_short() {

    Integer maxAge = getResponse("/").getMaxAge();

    assertThat(maxAge)
        .isEqualTo(10);
  }

  @Test
  void max_age_of_entry_point_without_timestamp_should_be_long() {

    Integer maxAge = getResponse("/?timestamp=foo").getMaxAge();

    assertThat(maxAge)
        .isEqualTo(Duration.ofDays(100).getSeconds());
  }

  @Test
  void entry_point_should_add_current_timestamp_query_to_all_links() {

    HalResponse entryPoint = getResponse("/");

    List<UriComponents> linkUris = getAllLinksAsUriComponents(entryPoint);

    assertThat(linkUris)
        .extracting(UriComponents::getQueryParams)
        .allMatch(queryParams -> !queryParams.getFirst("timestamp").isEmpty());
  }

  @Test
  void entry_point_should_add_incoming_timestamp_query_to_all_links() {

    HalResponse entryPoint = getResponse("/?timestamp=foo");

    List<UriComponents> linkUris = getAllLinksAsUriComponents(entryPoint);

    assertThat(linkUris)
        .extracting(UriComponents::getQueryParams)
        .allMatch(queryParams -> "foo".equals(queryParams.getFirst("timestamp")));
  }

  // it's worth repeating the max-age tests for the resource that
  // uses the HAL client internally to fetch other resources, because
  // the max-age will depend on the max-age of those upstream resources

  @Test
  void max_age_of_detailed_employee_without_timestamp_should_be_short() {

    Integer maxAge = getResponse("/employees/6/detailed").getMaxAge();

    assertThat(maxAge)
        .isEqualTo(10);
  }

  @Test
  void max_age_of_detailed_employee_with_timestamp_should_be_long() {

    Integer maxAge = getResponse("/employees/6/detailed?timestamp=foo").getMaxAge();

    assertThat(maxAge)
        .isEqualTo(Duration.ofDays(100).getSeconds());
  }

}
