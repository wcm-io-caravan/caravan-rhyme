package io.wcm.caravan.rhyme.examples.movies.impl;


import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.awslambda.api.LambdaRhyme;
import io.wcm.caravan.rhyme.awslambda.api.paging.AbstractPagingResource;
import io.wcm.caravan.rhyme.examples.movies.api.ApiEntryPoint;
import io.wcm.caravan.rhyme.examples.movies.api.SearchResult;
import io.wcm.caravan.rhyme.examples.movies.api.SearchResultPage;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Server-side resource implementation of the {@link SearchResultPage} HAL API interface.
 * It uses {@link MovieSearch} to execute the actual search, and extends {@link AbstractPagingResource} to implement the
 * paging logic.
 */
class MovieSearchResultResource extends AbstractPagingResource<SearchResultPage, SearchResult> implements SearchResultPage {

  static final String PATH = "/results";
  private static final int RESULTS_PER_PAGE = 5;

  private static final String PARAM_PAGE = "page";
  private static final String PARAM_SEARCH_TERM = ApiEntryPoint.SEARCH_TERM;

  private final LambdaRhyme rhyme;
  private final String searchTerm;

  MovieSearchResultResource(LambdaRhyme rhyme, String searchTerm, Integer page) {
    super(page, RESULTS_PER_PAGE);
    this.rhyme = rhyme;
    this.searchTerm = searchTerm;
  }

  static MovieSearchResultResource createWithRequestParametersFrom(LambdaRhyme rhyme) {

    Map<String, String> queryParameters = ObjectUtils.defaultIfNull(rhyme.getRequest().getQueryStringParameters(), Collections.emptyMap());

    int page = NumberUtils.toInt(queryParameters.get(PARAM_PAGE));

    String searchTerm = queryParameters.get(PARAM_SEARCH_TERM);
    if (StringUtils.isBlank(searchTerm)) {
      throw new HalApiServerException(HttpStatusCode.BAD_REQUEST, "You must specifiy a value for the query parameter " + PARAM_SEARCH_TERM);
    }

    return new MovieSearchResultResource(rhyme, searchTerm, page);
  }

  @Override
  protected Stream<SearchResult> getStreamOfAllItems() {
    // the super class will extract the items to be shown on the current page (plus one more to check if a 'next' link should be present)
    return new MovieSearch(rhyme).findMovies(searchTerm);
  }

  @Override
  protected SearchResultPage createLinkedPage(int linkedPage) {
    // this is called by the super class to create the optional next/prev/first links
    return new MovieSearchResultResource(rhyme, searchTerm, linkedPage);
  }

  @Override
  public SearchResultPage withNewSearchTerm(String newSearchTerm) {
    // create a link template to the first page for a different search term (which is null when this resource is rendered)
    return new MovieSearchResultResource(rhyme, newSearchTerm, 0);
  }

  @Override
  public String getSearchTerm() {
    return searchTerm;
  }

  @Override
  public Link createLink() {
    // this will create either a resolved link, or a template (if null was passed for one of the parameters in the constructor)
    return rhyme.buildLinkTo(PATH)
        .addQueryVariable(PARAM_SEARCH_TERM, searchTerm)
        .addQueryVariable(PARAM_PAGE, getCurrentPageNumber())
        .build();
  }
}
