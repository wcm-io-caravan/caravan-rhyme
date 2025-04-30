package io.wcm.caravan.rhyme.examples.movies.api;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.FIRST;
import static io.wcm.caravan.rhyme.api.relations.StandardRelations.LAST;
import static io.wcm.caravan.rhyme.api.relations.StandardRelations.NEXT;
import static io.wcm.caravan.rhyme.api.relations.StandardRelations.PREV;

import java.util.Optional;
import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * A browsable page of movies in the database, sorted by their IMDB ranking. The number of elements per page can be
 * specified in the link template from the API's entry point.
 */
@HalApiInterface
public interface MoviesPage extends LinkableResource {

  /**
   * @return information about the current page and total number of movies
   */
  @ResourceState
  PagingInfo getPagingInfo();

  /**
   * @return embedded resources for the movies from the current page
   */
  @Related("movies")
  Stream<Movie> getPageContent();

  /**
   * @return a link to the first page of movies (not present on the first page)
   */
  @Related(FIRST)
  Optional<MoviesPage> getFirstPage();

  /**
   * @return a link to the previous page of movies (not present on the first page)
   */
  @Related(PREV)
  Optional<MoviesPage> getPrevPage();

  /**
   * @return a link to the next page of movies (not present on the last page)
   */
  @Related(NEXT)
  Optional<MoviesPage> getNextPage();

  /**
   * @return a link to the last page of movies (not present on the last page)
   */
  @Related(LAST)
  Optional<MoviesPage> getLastPage();
}
