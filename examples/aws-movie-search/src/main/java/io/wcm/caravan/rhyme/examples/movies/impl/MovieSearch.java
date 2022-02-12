package io.wcm.caravan.rhyme.examples.movies.impl;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import io.wcm.caravan.rhyme.awslambda.api.LambdaRhyme;
import io.wcm.caravan.rhyme.awslambda.api.paging.AbstractPagingResource;
import io.wcm.caravan.rhyme.awslambda.api.paging.PagingUtils;
import io.wcm.caravan.rhyme.examples.movies.api.Director;
import io.wcm.caravan.rhyme.examples.movies.api.Movie;
import io.wcm.caravan.rhyme.examples.movies.api.MoviesDemoApi;
import io.wcm.caravan.rhyme.examples.movies.api.MoviesPage;
import io.wcm.caravan.rhyme.examples.movies.api.SearchResult;

/**
 * Implements the search functionality and all interaction with the upstream API.
 */
class MovieSearch {

  /** the name of the AWS staging variable to configure the URL of the data source */
  static final String UPSTREAM_URL_VARIABLE = "moviesDemoEntryPointUrl";
  /** the default URL of the data source if no such staging variable is present */
  static final String UPSTREAM_URL_DEFAULT = "https://hypermedia-movies-demo.herokuapp.com/api";

  private final LambdaRhyme rhyme;

  MovieSearch(LambdaRhyme rhyme) {
    this.rhyme = rhyme;
  }

  /**
   * @return a dynamic client proxy to load the entry point of the Hypermedia Movies Demo API
   */
  MoviesDemoApi getUpstreamEntryPoint() {

    // the URL for the entry point can be overridden for different AWS stages (or integration tests)
    String entryPointUrl = rhyme.getRequest().getStageVariables()
        .getOrDefault(UPSTREAM_URL_VARIABLE, UPSTREAM_URL_DEFAULT);

    return rhyme.getRemoteResource(entryPointUrl, MoviesDemoApi.class);
  }

  /**
   * Executes a case-insensitive text search across all available movies from the upstream API.
   * If you are using {@link Stream#limit(long)} on the returned stream (as the paging logic in
   * {@link AbstractPagingResource} does), only the required amount
   * of movie pages from the upstream API will be loaded.
   * @param searchTerm the text to search for in the title of the movie and names of their directors
   * @return a lazy-loading {@link Stream} that provides one {@link SearchResult} for each movie matching the search
   *         term
   */
  Stream<SearchResult> findMovies(String searchTerm) {

    // create a client proxy to load the first page of up to 25 movies
    MoviesPage firstPage = getUpstreamEntryPoint().getMoviesPage(0, 25);

    // Recursively follow the "next" links in the page resources to create a Stream that lazily loads all pages of movies
    Stream<Movie> streamOfAllMovies = PagingUtils.createLazyAutoPagingStream(firstPage, MoviesPage::getPageContent, MoviesPage::getNextPage);

    // return a result for each of the movies that it is considered a search hit
    return streamOfAllMovies.flatMap(movie -> createResultIfMovieMatchesSearchTerm(movie, searchTerm));
  }

  private Stream<SearchResult> createResultIfMovieMatchesSearchTerm(Movie movie, String searchTerm) {

    // provide a search result if the search term can be found in the movie's title
    String title = movie.getTitle();
    if (StringUtils.containsIgnoreCase(title, searchTerm)) {
      return Stream.of(createResult(movie, "a movie ranked " + movie.getRank() + " on IMDB"));
    }

    // otherwise provide a search result only if a director's name contains the search term
    return movie.getDirectors()
        // we can avoid actually loading the director's resource by reading the director's name from the link to the resource
        .filter(director -> StringUtils.containsIgnoreCase(director.createLink().getName(), searchTerm))
        .map(director -> createResult(movie, "a movie directed by " + director.createLink().getName()))
        // even if multiple directors of this movie match the search term, we want the movie to only show up once
        .limit(1);
  }

  private SearchResult createResult(Movie movie, String description) {
    // create the embedded resource, with links to the upstream service being provided by the client proxies
    return new SearchResult() {

      @Override
      public String getTitle() {
        return movie.getTitle();
      }

      @Override
      public String getDescription() {
        return description;
      }

      @Override
      public Movie getMovie() {
        return movie;
      }

      @Override
      public Stream<Director> getDirectors() {
        return movie.getDirectors();
      }
    };
  }
}
