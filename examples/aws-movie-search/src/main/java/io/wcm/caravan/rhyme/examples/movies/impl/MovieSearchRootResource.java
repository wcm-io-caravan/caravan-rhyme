package io.wcm.caravan.rhyme.examples.movies.impl;

import java.time.Duration;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.awslambda.api.LambdaRhyme;
import io.wcm.caravan.rhyme.examples.movies.api.ApiEntryPoint;
import io.wcm.caravan.rhyme.examples.movies.api.MoviesDemoApi;
import io.wcm.caravan.rhyme.examples.movies.api.SearchResultPage;

/**
 * Server-side resource implementation of the {@link ApiEntryPoint} HAL API interface.
 */
class MovieSearchRootResource implements ApiEntryPoint {

  static final String PATH = "/";

  private final LambdaRhyme rhyme;

  MovieSearchRootResource(LambdaRhyme rhyme) {
    this.rhyme = rhyme;

    // allow the entry point to be cached, but only for a short amount of time
    rhyme.setResponseMaxAge(Duration.ofMinutes(1));
  }

  @Override
  public SearchResultPage getSearchResults(String searchTerm) {

    return new MovieSearchResultResource(rhyme, searchTerm, 0);
  }

  @Override
  public MoviesDemoApi getUpstreamEntryPoint() {

    // to create the link, we can simply return the dynamic client proxy that is also used to fetch data from there
    return new MovieSearch(rhyme).getUpstreamEntryPoint();
  }

  @Override
  public Link createLink() {

    return rhyme.buildLinkTo(PATH)
        .build();
  }
}
