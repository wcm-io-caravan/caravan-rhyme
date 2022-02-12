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
 * A browseable page of directors in the database. The number of elements per page can be specified in the
 * link template from the API's entry point.
 */
@HalApiInterface
public interface DirectorsPage extends LinkableResource {

  /**
   * @return information about the current page and total number of directors
   */
  @ResourceState
  PagingInfo getPagingInfo();

  /**
   * @return embedded resources for the directors from the current page
   */
  @Related("directors")
  Stream<Director> getPageContent();

  /**
   * @return a link to the first page of directors (not present on the first page)
   */
  @Related(FIRST)
  Optional<DirectorsPage> getFirstPage();

  /**
   * @return a link to the previous page of directors (not present on the first page)
   */
  @Related(PREV)
  Optional<DirectorsPage> getPrevPage();

  /**
   * @return a link to the next page of directors (not present on the last page)
   */
  @Related(NEXT)
  Optional<DirectorsPage> getNextPage();

  /**
   * @return a link to the last page of directors (not present on the last page)
   */
  @Related(LAST)
  Optional<DirectorsPage> getLastPage();
}
