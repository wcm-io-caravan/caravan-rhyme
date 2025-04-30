package io.wcm.caravan.rhyme.examples.movies.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoviesDemoStubTest {

  private MoviesDemoStub stub = new MoviesDemoStub();

  @Test
  void test_paging_info_can_be_loaded() {

    stub.addMovies(200);

    MoviesPage firstPage = stub.getMoviesPage(0, 10);

    PagingInfo paging = firstPage.getPagingInfo();

    assertThat(paging.getNumber())
        .isZero();

    assertThat(paging.getSize())
        .isEqualTo(10);

    assertThat(paging.getTotalElements())
        .isEqualTo(200);

    assertThat(paging.getTotalPages())
        .isEqualTo(20);
  }
}
