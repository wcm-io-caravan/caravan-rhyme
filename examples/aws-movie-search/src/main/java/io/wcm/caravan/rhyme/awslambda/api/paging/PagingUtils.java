package io.wcm.caravan.rhyme.awslambda.api.paging;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods to simplify streaming of multi-page HAL resource collections
 */
public final class PagingUtils {

  private PagingUtils() {
    // static methods only
  }

  /**
   * Create a lazy-loading stream of all items from several linked HAL resource collection pages. It will initially load
   * the content of the first page only, but if more items are consumed from the stream, it will load as many of the
   * following pages as are available (and/or required, as the lazy-loading will stop at some point if
   * {@link Stream#limit(long)} is used).
   * @param <P> a type representing a page of items
   * @param <I> the type of the individual items on the page
   * @param firstPage a client-proxy that loads the first page
   * @param pageContentFunc provides a {@link Stream} of items on a given page
   * @param nextPageFunc returns the next page (or empty if there is no next page)
   * @return a {@link Stream} that will lazily load all items on every page
   */
  public static <P, I> Stream<I> createLazyAutoPagingStream(P firstPage, Function<P, Stream<I>> pageContentFunc, Function<P, Optional<P>> nextPageFunc) {

    Iterator<I> iterator = new AutoPagingIterator<>(firstPage, pageContentFunc, nextPageFunc);

    Spliterator<I> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE | Spliterator.ORDERED);

    return StreamSupport.stream(spliterator, false);
  }

  static class AutoPagingIterator<P, R> implements Iterator<R> {

    private final Function<P, Stream<R>> pageContentFunc;
    private final Function<P, Optional<P>> nextPageFunc;

    private final Queue<R> itemsOnCurrentPage = new LinkedList<>();
    private P currentPage;

    AutoPagingIterator(P firstPage, Function<P, Stream<R>> pageContentFunc, Function<P, Optional<P>> nextPageFunc) {

      this.pageContentFunc = pageContentFunc;
      this.nextPageFunc = nextPageFunc;

      switchCurrentPageAndLoadItems(firstPage);
    }

    private void switchCurrentPageAndLoadItems(P nextPage) {

      currentPage = nextPage;

      pageContentFunc.apply(currentPage)
          .forEach(itemsOnCurrentPage::add);
    }

    @Override
    public boolean hasNext() {

      if (!itemsOnCurrentPage.isEmpty()) {
        return true;
      }

      Optional<P> nextPage = nextPageFunc.apply(currentPage);

      nextPage.ifPresent(this::switchCurrentPageAndLoadItems);

      return !itemsOnCurrentPage.isEmpty();
    }

    @Override
    public R next() {

      if (itemsOnCurrentPage.isEmpty()) {
        throw new NoSuchElementException("next() was called even though #hasNext() must have returned false");
      }

      return itemsOnCurrentPage.poll();
    }
  }
}
