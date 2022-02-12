package io.wcm.caravan.rhyme.examples.movies.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import io.wcm.caravan.LambdaIntegrationTestClient;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.examples.movies.api.ApiEntryPoint;
import io.wcm.caravan.rhyme.examples.movies.api.MoviesDemoApi;
import io.wcm.caravan.rhyme.examples.movies.api.MoviesDemoStub;
import io.wcm.caravan.rhyme.examples.movies.api.SearchResult;
import io.wcm.caravan.rhyme.examples.movies.api.SearchResultPage;
import software.amazon.awssdk.http.HttpStatusCode;

/**
 * An integration test for {@link MovieSearchRequestHandler} that is using the {@link MoviesDemoStub} implementation
 * to have full control over simulated responses from the upstream service.
 */
class MovieSearchLambdaTest {

  private final LambdaIntegrationTestClient testClient;

  private final ApiEntryPoint api;

  private final MoviesDemoStub moviesStub = new MoviesDemoStub();

  MovieSearchLambdaTest() {

    // create the instance under test with a remote resource override
    MovieSearchRequestHandler requestHandlerWithStubs = new MovieSearchRequestHandler() {

      @Override
      protected RhymeBuilder configureRhymeBuilder(RhymeBuilder builder, APIGatewayProxyRequestEvent request) {

        return super.configureRhymeBuilder(builder, request)
            // ensure that the stub implementation of MoviesDemoApi is used instead of the dynamic client proxies
            // that execute actual upstream HTTP requests
            .withRemoteResourceOverride(MoviesDemoStub.BASE_URL, MoviesDemoApi.class, metrics -> moviesStub);
      }
    };

    testClient = new LambdaIntegrationTestClient(requestHandlerWithStubs);

    // make sure that the search is actually using the alternative entry point URL for which we defined the stubbing above
    testClient.getStageVariables().put(MovieSearch.UPSTREAM_URL_VARIABLE, MoviesDemoStub.BASE_URL);

    api = testClient.getEntryPoint(ApiEntryPoint.class);
  }

  @Test
  void entry_point_can_be_loaded() {

    HalResponse response = testClient.getResponse(api);

    assertThat(response.getStatus())
        .isEqualTo(200);
  }

  @Test
  void entry_point_contains_metadata() {

    HalResponse response = testClient.getResponse(api);

    assertThat(response.getBody().getEmbeddedResource("rhyme:metadata"))
        .isNotNull();
  }

  @Test
  void entry_point_has_link_to_upstream_movies_api() {

    Link upstreamLink = api.getUpstreamEntryPoint().createLink();

    assertThat(upstreamLink.getHref())
        .isEqualTo(MoviesDemoStub.BASE_URL);
  }

  @Test
  void resolved_result_page_contains_search_term() {

    String searchTerm = api.getSearchResults("The").getSearchTerm();

    assertThat(searchTerm)
        .isEqualTo("The");
  }

  @Test
  void responds_with_bad_request_if_search_term_is_empty() {

    SearchResultPage firstPage = api.getSearchResults("");

    HalApiClientException ex = assertThrows(HalApiClientException.class, firstPage::getPageContent);

    assertThat(ex.getStatusCode())
        .isEqualTo(HttpStatusCode.BAD_REQUEST);
  }

  @Test
  void resolved_result_page_is_empty_if_search_term_doesnt_match() {

    moviesStub.addMovies(10);

    SearchResultPage firstPage = api.getSearchResults("Not present in any title");

    assertThat(firstPage.getPageContent())
        .isEmpty();

    assertThat(firstPage.getNextPage())
        .isNotPresent();
  }

  @Test
  void first_result_page_contains_up_to_5_results_and_link_to_next_page() {

    moviesStub.addMovies(10);

    SearchResultPage firstPage = api.getSearchResults("Movie");

    assertThat(firstPage.getPageContent())
        .hasSize(5)
        .extracting(SearchResult::getTitle)
        .containsExactlyElementsOf(createExpectedMovieMatchTitles(0, 1, 2, 3, 4));

    assertThat(firstPage.getNextPage())
        .isPresent();
  }

  @Test
  void second_result_page_contains_remaining_results() {

    moviesStub.addMovies(10);

    SearchResultPage secondPage = api.getSearchResults("Movie").getNextPage().get();

    assertThat(secondPage.getPageContent())
        .hasSize(5)
        .extracting(SearchResult::getTitle)
        .containsExactlyElementsOf(createExpectedMovieMatchTitles(5, 6, 7, 8, 9));

    assertThat(secondPage.getNextPage())
        .isNotPresent();
  }

  @Test
  void results_contain_matches_with_director_names() {

    moviesStub.addMovies(5);

    SearchResultPage firstPage = api.getSearchResults(MoviesDemoStub.DEFAULT_DIRECTOR_NAME);

    assertThat(firstPage.getPageContent())
        .hasSize(5)
        .extracting(SearchResult::getDescription)
        .containsExactlyElementsOf(createExpectedDirectorMatchDescriptions(0, 1, 2, 3, 4));
  }

  private List<String> createExpectedMovieMatchTitles(Integer... indices) {

    return Stream.of(indices)
        .map(i -> "Movie #" + i)
        .collect(Collectors.toList());
  }

  private List<String> createExpectedDirectorMatchDescriptions(Integer... indices) {

    return Stream.of(indices)
        .map(i -> "a movie directed by " + MoviesDemoStub.DEFAULT_DIRECTOR_NAME)
        .collect(Collectors.toList());
  }

  @Test
  void returns_404_error_response_for_unknown_paths() {

    HalApiClientException ex = assertThrows(HalApiClientException.class, () -> testClient.getResponse("/foo/bar"));

    assertThat(ex.getStatusCode())
        .isEqualTo(404);
  }
}
