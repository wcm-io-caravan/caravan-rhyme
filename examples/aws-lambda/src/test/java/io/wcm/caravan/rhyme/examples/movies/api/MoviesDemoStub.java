package io.wcm.caravan.rhyme.examples.movies.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.awslambda.api.paging.AbstractPagingResource;

/**
 * A stub implementation of {@link MoviesDemoApi} to be used in unit and integration test.
 * It initializes with an empty database of movies, and you need to use {@link #addMovies(int)}
 * to define the movies that should be returned.
 */
public class MoviesDemoStub implements MoviesDemoApi {

  public static final String BASE_URL = "https://stubbed.upstream/api";

  public static final String DEFAULT_DIRECTOR_NAME = "Foo Bar";

  private final List<Movie> movies = new ArrayList<>();

  private final List<Director> directors = new ArrayList<>();

  /**
   * Creates multiple movie entries in the database (using "Movie #0", "Movie #1" etc as titles,
   * and a director named {@value #DEFAULT_DIRECTOR_NAME})
   * @param count the total number of movies to be returned
   */
  public void addMovies(int count) {

    if (directors.isEmpty()) {
      directors.add(new DirectorImpl(0, DEFAULT_DIRECTOR_NAME));
    }

    IntStream.range(0, count)
        .mapToObj(i -> new MovieImpl(i, directors.get(0)))
        .forEach(movies::add);
  }

  @Override
  public MoviesPage getMoviesPage(Integer page, Integer size) {

    return new MoviesPageImpl(page, size);
  }

  @Override
  public DirectorsPage getDirectorsPage(Integer page, Integer size) {

    throw new HalApiDeveloperException("Not implemented");
  }

  @Override
  public Link createLink() {

    return new Link(BASE_URL);
  }

  class MoviesPageImpl extends AbstractPagingResource<MoviesPage, Movie> implements MoviesPage {

    MoviesPageImpl(Integer page, Integer size) {
      super(page, size);
    }

    @Override
    public PagingInfo getPagingInfo() {

      return new PagingInfo(getCurrentPageNumber(), (int)getPageContent().count(), movies.size(), (movies.size() - 1) / getMaxPageSize() + 1);
    }

    @Override
    public Optional<MoviesPage> getLastPage() {

      return Optional.of(createLinkedPage((movies.size() - 1) / getMaxPageSize()));
    }

    @Override
    protected Stream<Movie> getStreamOfAllItems() {

      return movies.stream();
    }

    @Override
    protected MoviesPage createLinkedPage(int linkedPage) {

      return new MoviesPageImpl(linkedPage, getMaxPageSize());
    }

    @Override
    public Link createLink() {

      return new Link(BASE_URL + "/movies");
    }
  }

  class DirectorImpl implements Director {

    private final int index;
    private final String name;

    DirectorImpl(int index, String name) {
      this.index = index;
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Stream<Movie> getMovies() {

      return movies.stream()
          .filter(m -> m.getDirectors().anyMatch(d -> d.createLink().getName().equals(name)));
    }

    @Override
    public Link createLink() {
      return new Link(BASE_URL + "/directors/" + index)
          .setName(name);
    }
  }

  class MovieImpl implements Movie {

    private final int index;
    private final Director[] movieDirectors;

    MovieImpl(int index, Director... directors) {
      this.index = index;
      this.movieDirectors = directors;
    }

    @Override
    public String getTitle() {
      return "Movie #" + index;
    }

    @Override
    public int getYear() {
      return 1970 + (7123 * index) % 50;
    }

    @Override
    public float getRating() {
      return 9.9f - index * 0.01f;
    }

    @Override
    public int getRank() {
      return index + 1;
    }

    @Override
    public String getImdbId() {
      return "tt" + (456245641 * index) % 10000000;
    }

    @Override
    public String getThumb() {
      return "/movie-data/thumbs/" + getImdbId() + ".jpg";
    }

    @Override
    public Stream<Director> getDirectors() {
      return Stream.of(movieDirectors);
    }

    @Override
    public Link createLink() {
      return new Link(BASE_URL + "/movies/" + index);
    }
  }
}
