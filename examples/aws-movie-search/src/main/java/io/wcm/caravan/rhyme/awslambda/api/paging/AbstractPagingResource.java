package io.wcm.caravan.rhyme.awslambda.api.paging;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * An abstract base class for a {@link LinkableResource} that allows browsing through multiple pages of embedded HAL
 * resources.
 * <p>
 * You have to implement {@link #getStreamOfAllItems()} to provide all the resources that should be embedded, and this
 * class will then use {@link Stream#skip(long)} and {@link Stream#limit(long)} to only extract the items to be shown on
 * the current page.
 * </p>
 * <p>
 * {@link #createLinkedPage(int)} will be called to create the resources that should be linked to through the "next",
 * "prev" and "first" links.
 * </p>
 * @param <P> a {@link HalApiInterface} representing a page of items
 * @param <I> a {@link HalApiInterface} representing an embedded item on each page
 */
public abstract class AbstractPagingResource<P, I> {

  private final Integer currentPage;
  private final long maxPageSize;

  private List<I> itemsOnPagePlusOne;

  /**
   * @param currentPage the 0-based index of the current page (can be null if used to generate a link template)
   * @param maxPageSize the maximum number of items per page
   */
  protected AbstractPagingResource(Integer currentPage, int maxPageSize) {
    this.currentPage = currentPage;
    this.maxPageSize = maxPageSize;
  }

  /**
   * @return a (potentially infinite) stream of all items to be embedded across all pages
   */
  protected abstract Stream<I> getStreamOfAllItems();

  /**
   * @param linkedPage the (0-based) index of the page to be linked
   * @return an implementation of the page to be linked
   */
  protected abstract P createLinkedPage(int linkedPage);

  private List<I> getItemsOnPagePlusOne() {

    // ensure that the potentially expensive call to #getStreamOfAllItems is only executed once
    if (itemsOnPagePlusOne == null) {
      itemsOnPagePlusOne = getStreamOfAllItems()
          // ignore the items from previous pages
          .skip(maxPageSize * currentPage)
          // extract one more item than included on the current page (to be able to determine if there is a next page)
          .limit(maxPageSize + 1)
          .collect(Collectors.toList());
    }

    return itemsOnPagePlusOne;
  }

  private void ensureThatCurrentPageIsNotNull() {

    if (currentPage == null) {
      throw new HalApiDeveloperException("A null value was specified as 'currentPage' in the constructor. "
          + "This is valid to create a link template, but you cannot call any of the paging methods.");
    }
  }

  /**
   * @return a {@link Stream} of all items to show on the current page
   */
  public Stream<I> getPageContent() {

    ensureThatCurrentPageIsNotNull();

    return getItemsOnPagePlusOne().stream()
        .limit(maxPageSize);
  }

  /**
   * @return the next page (empty if the current page already include the last available items)
   */
  public Optional<P> getNextPage() {

    ensureThatCurrentPageIsNotNull();

    if (getItemsOnPagePlusOne().size() <= maxPageSize) {
      return Optional.empty();
    }

    return Optional.of(createLinkedPage(currentPage + 1));
  }

  /**
   * @return the first page (empty if the current page is the first page)
   */
  public Optional<P> getFirstPage() {

    ensureThatCurrentPageIsNotNull();

    return createPreviousPage(0);
  }

  /**
   * @return the previous page (empty if the current page is the first page)
   */
  public Optional<P> getPrevPage() {

    ensureThatCurrentPageIsNotNull();

    return createPreviousPage(currentPage - 1);
  }

  private Optional<P> createPreviousPage(int index) {

    if (currentPage == 0) {
      return Optional.empty();
    }

    return Optional.of(createLinkedPage(index));
  }

  /**
   * @return the 0-based index of the current page
   */
  public Integer getCurrentPageNumber() {
    return currentPage;
  }

  /**
   * @return the maximum number of items per page
   */
  public int getMaxPageSize() {
    return (int)maxPageSize;
  }
}
