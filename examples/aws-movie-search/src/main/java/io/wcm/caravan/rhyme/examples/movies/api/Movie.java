package io.wcm.caravan.rhyme.examples.movies.api;

import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * A resource representing a movie in the <a href="https://hypermedia-movies-demo.herokuapp.com/">Hypermedia Movies
 * Demo</a> database
 */
@HalApiInterface
public interface Movie extends LinkableResource {

  /**
   * @return the title of the movie
   */
  @ResourceProperty
  String getTitle();

  /**
   * @return the year when this movie premiered
   */
  @ResourceProperty
  int getYear();

  /**
   * @return the IMDB rating of the movie (ranged between 0 and 10)
   */
  @ResourceProperty
  float getRating();

  /**
   * @return the rank of the movies (when sorted by rating)
   */
  @ResourceProperty
  int getRank();

  /**
   * @return the ID in the IMDB database
   */
  @ResourceProperty
  String getImdbId();

  /**
   * @return an absolute path of a thumbnail image (available on https://hypermedia-movies-demo.herokuapp.com)
   */
  @ResourceProperty
  String getThumb();

  /**
   * @return link(s) to the director(s) of this movie
   */
  @Related("directors")
  Stream<Director> getDirectors();
}
