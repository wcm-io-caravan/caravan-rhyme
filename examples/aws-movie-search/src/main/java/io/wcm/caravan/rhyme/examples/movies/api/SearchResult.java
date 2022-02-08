package io.wcm.caravan.rhyme.examples.movies.api;

import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;

/**
 * A single result that is embedded into a search result page
 */
@HalApiInterface
public interface SearchResult extends EmbeddableResource {

  /**
   * @return the title of the movie (with an explanation why it's shown on the current page)
   */
  @ResourceProperty
  String getTitle();

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
