package io.wcm.caravan.rhyme.examples.movies.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information on paging status and overall number of elements in the {@link MoviesPage} and {@link DirectorsPage}
 */
public class PagingInfo {

  private final int number;
  private final int size;
  private final int totalElements;
  private final int totalPages;

  @JsonCreator
  public PagingInfo(
      @JsonProperty("number") int number, @JsonProperty("size") int size,
      @JsonProperty("totalElements") int totalElements, @JsonProperty("totalPages") int totalPages) {

    this.number = number;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
  }

  /**
   * @return the 0-based number of the current page
   */
  public int getNumber() {
    return number;
  }

  /**
   * @return the number of embedded resources on the current page
   */
  public int getSize() {
    return size;
  }

  /**
   * @return the total number of elements in the database
   */
  public int getTotalElements() {
    return totalElements;
  }

  /**
   * @return the total number of pages (with the given page size)
   */
  public int getTotalPages() {
    return totalPages;
  }
}
