package io.wcm.caravan.rhyme.awslambda.api.paging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import io.wcm.caravan.rhyme.awslambda.api.paging.PagingUtils;
import io.wcm.caravan.rhyme.awslambda.api.paging.PagingUtils.AutoPagingIterator;

@ExtendWith(MockitoExtension.class)
class PagingUtilsTest {

  private static final int ITEMS_PER_PAGE = 5;

  interface PageOfNumbers {

    Stream<Integer> getPageContent();

    Optional<PageOfNumbers> getNextPage();
  }

  private static class PageOfNumbersImpl implements PageOfNumbers {

    private final List<Integer> numbers;

    PageOfNumbersImpl(int count) {
      numbers = IntStream.range(0, count)
          .mapToObj(Integer::new)
          .collect(Collectors.toList());
    }

    private PageOfNumbersImpl(List<Integer> remainingNumbers) {
      this.numbers = remainingNumbers;
    }

    @Override
    public Stream<Integer> getPageContent() {

      return numbers.stream()
          .limit(ITEMS_PER_PAGE);
    }

    @Override
    public Optional<PageOfNumbers> getNextPage() {

      List<Integer> remaining = numbers.stream()
          .skip(ITEMS_PER_PAGE)
          .collect(Collectors.toList());

      if (remaining.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(new PageOfNumbersImpl(remaining));

    }
  }

  private static Stream<Integer> createAutoPagingStream(PageOfNumbers firstPage) {

    return PagingUtils.createLazyAutoPagingStream(firstPage, PageOfNumbers::getPageContent, PageOfNumbers::getNextPage);
  }

  static List<Integer> getExpectedItems(int numItems) {

    return IntStream.range(0, numItems)
        .mapToObj(Integer::new)
        .collect(Collectors.toList());
  }

  @Test
  void should_return_empty_stream_if_first_page_is_empty() {

    PageOfNumbers emptyPage = new PageOfNumbersImpl(0);

    Stream<Integer> stream = createAutoPagingStream(emptyPage);

    assertThat(stream)
        .isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = { 1,
      ITEMS_PER_PAGE - 1, ITEMS_PER_PAGE, ITEMS_PER_PAGE + 1,
      2 * ITEMS_PER_PAGE - 1, 2 * ITEMS_PER_PAGE, 2 * ITEMS_PER_PAGE + 1,
      523 })
  void should_return_items_from_all_pages_without_gaps_in_correct_order(int numItems) {

    PageOfNumbers firstPage = new PageOfNumbersImpl(numItems);

    Stream<Integer> stream = createAutoPagingStream(firstPage);

    assertThat(stream)
        .containsExactlyElementsOf(getExpectedItems(numItems));
  }

  @ParameterizedTest
  @ValueSource(ints = { 1, ITEMS_PER_PAGE - 1, ITEMS_PER_PAGE })
  void getNextPage_is_not_called_if_limit_is_less_then_page_size(int limit) {

    PageOfNumbers firstPage = spy(new PageOfNumbersImpl(10));

    Stream<Integer> limitedStream = createAutoPagingStream(firstPage)
        .limit(limit);

    assertThat(limitedStream)
        .containsExactlyElementsOf(getExpectedItems(limit));

    verify(firstPage, never()).getNextPage();
  }

  @ParameterizedTest
  @ValueSource(ints = { 1, 2, 3, 4, 5, 6 })
  void getNextPage_is_not_called_on_second_page_if_limit_is_less_then_combined_size(int limit) {

    PageOfNumbers firstPage = new PageOfNumbers() {

      @Override
      public Stream<Integer> getPageContent() {

        return Stream.of(0, 1, 2);
      }

      @Override
      public Optional<PageOfNumbers> getNextPage() {
        return Optional.of(new PageOfNumbers() {

          @Override
          public Stream<Integer> getPageContent() {
            return Stream.of(3, 4, 5);
          }

          @Override
          public Optional<PageOfNumbers> getNextPage() {
            throw new AssertionError("getNextPage() shouldn't be called");
          }
        });
      }
    };

    Stream<Integer> limitedStream = createAutoPagingStream(firstPage).limit(limit);

    assertThat(limitedStream)
        .containsExactlyElementsOf(getExpectedItems(limit));
  }

  @Test
  void returns_empty_stream_for_endless_chain_of_empty_pages() {

    PageOfNumbers firstPage = new PageOfNumbers() {

      @Override
      public Stream<Integer> getPageContent() {

        return Stream.empty();
      }

      @Override
      public Optional<PageOfNumbers> getNextPage() {
        return Optional.of(this);
      }

    };

    Stream<Integer> stream = createAutoPagingStream(firstPage);

    assertThat(stream)
        .isEmpty();
  }

  @Test
  void calling_next_on_iterator_fails_if_hasNext_return() {

    PageOfNumbers firstPage = new PageOfNumbersImpl(0);

    AutoPagingIterator<PageOfNumbers, Integer> it = new AutoPagingIterator<>(firstPage, PageOfNumbers::getPageContent, PageOfNumbers::getNextPage);

    NoSuchElementException ex = assertThrows(NoSuchElementException.class, it::next);

    assertThat(ex)
        .hasMessage("next() was called even though #hasNext() must have returned false");
  }
}
