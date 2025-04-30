package io.wcm.caravan.rhyme.examples.movies.api;

import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;

/**
 * A single movie search result that can only be retrieved as embedded resource within a search result page.
 */
@HalApiInterface
public interface SearchResult extends EmbeddableResource {

  /**
   * @return the title of the movie
   */
  @ResourceProperty
  String getTitle();

  /**
   * @return an explanation why the movie is shown on the current page
   */
  @ResourceProperty
  String getDescription();

  /**
   * @return a link to the details of the movie (from the Hypermedia Movie Demos API)
   */
  @Related("movie")
  Movie getMovie();

  /**
   * @return link(s) to the director(s) of the movie (from the Hypermedia Movie Demos API)
   */
  @Related("directors")
  Stream<Director> getDirectors();
}
