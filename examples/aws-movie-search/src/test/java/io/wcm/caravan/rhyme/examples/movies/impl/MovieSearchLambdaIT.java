package io.wcm.caravan.rhyme.examples.movies.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.damnhandy.uri.template.UriTemplate;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;

import io.wcm.caravan.ClasspathResourceSupport;
import io.wcm.caravan.LambdaIntegrationTestClient;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.relations.StandardRelations;
import io.wcm.caravan.rhyme.api.relations.VndErrorRelations;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.examples.movies.api.ApiEntryPoint;
import io.wcm.caravan.rhyme.examples.movies.api.SearchResultPage;

/**
 * An integration test for the {@link MovieSearchRequestHandler} that shows how to use stub responses
 * from the src/test/resources folder (instead of connecting to the upstream service by HTTP).
 * <p>
 * One advantage of this (compared to {@link MovieSearchLambdaTest}) is that HTTP response caching and max-age handling
 * of {@link HalApiClient} is covered by these tests as well (and that's why the test cases concentrate on those
 * aspects).
 * </p>
 */
class MovieSearchLambdaIT {

  /** the instance under test, which overrides the {@link HalResourceLoader} to be used for all upstream requests */
  private static final MovieSearchRequestHandler REQUEST_HANDLER = new MovieSearchRequestHandler() {

    @Override
    protected HalResourceLoaderBuilder configureResourceLoader() {

      String apiUrl = MovieSearch.UPSTREAM_URL_DEFAULT;

      // create and configure a HttpClientSupport instance that serves files from src/test/resources
      ClasspathResourceSupport testResources = new ClasspathResourceSupport();
      testResources.addStubResponseMapping(apiUrl, "/movies-api.json");
      testResources.addStubResponseMapping(apiUrl + "/movies?size=25&page=0", "/movies-page.json");

      // use the caching configuration from the super class, but replace the HTTP client
      return super.configureResourceLoader()
          .withCustomHttpClient(testResources);
    }
  };

  private final LambdaIntegrationTestClient testClient;

  private final ApiEntryPoint api;

  MovieSearchLambdaIT() {

    testClient = new LambdaIntegrationTestClient(REQUEST_HANDLER);

    api = testClient.getEntryPoint(ApiEntryPoint.class);
  }

  @Test
  void entry_point_has_short_max_age() {

    HalResponse response = testClient.getResponse(api);

    assertThat(response.getMaxAge())
        .isEqualTo(60);
  }

  @Test
  void entry_point_has_link_to_upstream_movies_api() {

    Link upstreamLink = api.getUpstreamEntryPoint().createLink();

    assertThat(upstreamLink.getHref())
        .isEqualTo(MovieSearch.UPSTREAM_URL_DEFAULT);
  }

  @Test
  void resolved_result_page_has_five_results() {

    SearchResultPage firstPage = api.getSearchResults("the");

    assertThat(firstPage.getPageContent())
        .hasSize(5);
  }

  @Test
  void resolved_result_page_has_max_age_of_one_hour() {

    SearchResultPage firstPage = api.getSearchResults("the");

    HalResponse response = testClient.getResponse(firstPage);

    assertThat(response.getMaxAge())
        .isEqualTo(3600);
  }

  @Test
  void should_fail_for_searchTerm_for_which_not_enough_stubbed_responses_are_available() {

    SearchResultPage resultPage = api.getSearchResults("Something not to be found on first page");

    HalApiClientException ex = assertThrows(HalApiClientException.class, resultPage::getPageContent);

    assertThat(ex.getStatusCode())
        .isEqualTo(404);

    assertThat(ex.getErrorResponse().getBody().getEmbedded(VndErrorRelations.ERRORS))
        .hasSize(2)
        .first()
        .extracting(hal -> hal.getModel().path("message").asText())
        .asString()
        .contains("?page=1&size=25 has failed with status code 404");
  }

  @Test
  void curies_link_should_lead_to_generated_rhymedocs() {

    HalResponse entryPoint = testClient.getResponse(api);

    Link curiesLink = entryPoint.getBody().getLink(StandardRelations.CURIES);

    assertThat(curiesLink)
        .isNotNull();

    String curiesHref = UriTemplate.expand(curiesLink.getHref(), ImmutableMap.of("rel", "search"));

    APIGatewayProxyResponseEvent response = testClient.getGatewayProxyResponse(curiesHref);

    assertThat(response.getStatusCode())
        .isEqualTo(200);

    assertThat(response.getHeaders())
        .containsEntry(HttpHeaders.CONTENT_TYPE, "text/html");

    assertThat(response.getBody())
        .contains("<html>");
  }
}
