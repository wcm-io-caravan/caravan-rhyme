package io.wcm.caravan.rhyme.examples.movies.api;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.FIRST;
import static io.wcm.caravan.rhyme.api.relations.StandardRelations.NEXT;
import static io.wcm.caravan.rhyme.api.relations.StandardRelations.PREV;
import static io.wcm.caravan.rhyme.examples.movies.api.ApiEntryPoint.SEARCH_TERM;

import java.util.Optional;
import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * A page of search results for a given search term, with links to navigate to further pages.
 */
@HalApiInterface
public interface SearchResultPage extends LinkableResource {

  /**
   * @return the text that was searched
   */
  @ResourceProperty
  String getSearchTerm();

  /**
   * @return the embedded resources representing the results from the search (up to 5 per page), with links to
   *         fetch details of the movie and its director(s)
   */
  @Related("movies:result")
  Stream<SearchResult> getPageContent();

  /**
   * @return a link to the first page of results (not present on the first page)
   */
  @Related(FIRST)
  Optional<SearchResultPage> getFirstPage();

  /**
   * @return a link to the previous page of results (not present on the first page)
   */
  @Related(PREV)
  Optional<SearchResultPage> getPrevPage();

  /**
   * @return a link to the next page of results (not present on the last page)
   */
  @Related(NEXT)
  Optional<SearchResultPage> getNextPage();

  /**
   * a link template to execute another search with a different term
   * @param searchTerm the new term to search for
   * @return the first page with results for the new term
   */
  @Related("movies:search")
  SearchResultPage withNewSearchTerm(@TemplateVariable(SEARCH_TERM) String searchTerm);
}
