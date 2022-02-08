package io.wcm.caravan.rhyme.examples.movies.api;

import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * A resource representing a director in the <a href="https://hypermedia-movies-demo.herokuapp.com/">Hypermedia Movies
 * Demo</a> database
 */
@HalApiInterface
public interface Director extends LinkableResource {

  /**
   * @return the full name of the director
   */
  @ResourceProperty
  String getName();

  /**
   * @return links to the movie(s) from this director
   */
  @Related("movies")
  Stream<Movie> getMovies();
}
