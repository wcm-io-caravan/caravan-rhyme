package io.wcm.caravan.rhyme.examples.movies.api;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * This is the root resource for Rhyme's AWS lambda example.
 * <p>
 * It provides a very simple text search over the titles of movies and names of directors from the
 * <a href="https://hypermedia-movies-demo.herokuapp.com/">Hypermedia Movies Demo</a> database.
 * </p>
 */
@HalApiInterface
public interface ApiEntryPoint extends LinkableResource {

  /** the name of the query parameter to pass the search term to the {@link SearchResultPage} */
  String SEARCH_TERM = "searchTerm";

  /**
   * A link template to execute a case-insensitive text search for movies
   * @param searchTerm the required text to search for (in the titles of movies and names of directors)
   * @return the first {@link SearchResultPage}
   */
  @Related("movies:search")
  SearchResultPage getSearchResults(@TemplateVariable(SEARCH_TERM) String searchTerm);

  /**
   * @return a link to the upstream Hypermedia Movies Demo API that provides the data for the search
   */
  @Related("movies:source")
  MoviesDemoApi getUpstreamEntryPoint();
}
