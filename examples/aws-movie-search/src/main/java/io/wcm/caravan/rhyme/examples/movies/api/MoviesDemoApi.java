package io.wcm.caravan.rhyme.examples.movies.api;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * The API of the <a href="https://hypermedia-movies-demo.herokuapp.com/">Hypermedia Movies Demo</a> application from
 * Kai Toedter.
 * @see <a href="https://github.com/toedter/movies-demo">Github project</a>
 */
@HalApiInterface
public interface MoviesDemoApi extends LinkableResource {

  /**
   * A link template to load a page of the movie database
   * @param page the 0-based number of the page to load
   * @param size the max. number of movies to show on one page
   * @return a {@link MoviesPage} with the given parameters
   */
  @Related("movies")
  MoviesPage getMoviesPage(@TemplateVariable("page") Integer page, @TemplateVariable("size") Integer size);

  /**
   * A link template to load a page of the directors database
   * @param page the 0-based number of the page to load
   * @param size the max. number of directors to show on one page
   * @return a {@link DirectorsPage} with the given parameters
   */
  @Related("directors")
  DirectorsPage getDirectorsPage(@TemplateVariable("page") Integer page, @TemplateVariable("size") Integer size);
}
