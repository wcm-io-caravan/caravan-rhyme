package io.wcm.caravan.rhyme.awslambda.api.paging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

class AbstractPagingResourceTest {

  private static final int ITEMS_PER_PAGE = 10;

  class PageOfNumbers extends AbstractPagingResource<PageOfNumbers, Integer> {

    private List<Integer> allItemsAcrossPages;

    private boolean streamWasAlreadyRequested;

    PageOfNumbers(int totalSize) {
      this(0, getRangeAsList(0, totalSize));
    }

    private PageOfNumbers(Integer pageNumber, List<Integer> allItemsAcrossPages) {
      super(pageNumber, ITEMS_PER_PAGE);
      this.allItemsAcrossPages = allItemsAcrossPages;
    }

    @Override
    protected Stream<Integer> getStreamOfAllItems() {

      if (streamWasAlreadyRequested) {
        fail("getInfiniteStreamOfResults() was called more than once on the same instance");
      }
      streamWasAlreadyRequested = true;

      return allItemsAcrossPages.stream();
    }

    @Override
    protected PageOfNumbers createLinkedPage(int linkedPage) {
      return new PageOfNumbers(linkedPage, allItemsAcrossPages);
    }
  }

  private static List<Integer> getRangeAsList(int startInclusive, int endExclusive) {

    return IntStream.range(
        startInclusive, endExclusive)
        .mapToObj(Integer::new)
        .collect(Collectors.toList());
  }

  @Test
  void should_handle_empty_stream() {

    PageOfNumbers emptyPage = new PageOfNumbers(0);

    assertThat(emptyPage.getPageContent())
        .isEmpty();

    assertThat(emptyPage.getFirstPage())
        .isNotPresent();

    assertThat(emptyPage.getPrevPage())
        .isNotPresent();

    assertThat(emptyPage.getNextPage())
        .isNotPresent();
  }

  @ParameterizedTest
  @ValueSource(ints = { 1, ITEMS_PER_PAGE - 1, ITEMS_PER_PAGE })
  void should_handle_streams_that_fit_on_one_page(int totalNumItems) {

    PageOfNumbers onlyPage = new PageOfNumbers(totalNumItems);

    assertThat(onlyPage.getPageContent())
        .hasSize(totalNumItems)
        .containsExactlyElementsOf(getRangeAsList(0, totalNumItems));

    assertThat(onlyPage.getFirstPage())
        .isNotPresent();

    assertThat(onlyPage.getPrevPage())
        .isNotPresent();

    assertThat(onlyPage.getNextPage())
        .isNotPresent();
  }

  @ParameterizedTest
  @ValueSource(ints = { ITEMS_PER_PAGE + 1, 2 * ITEMS_PER_PAGE - 1, 2 * ITEMS_PER_PAGE, })
  void should_handle_streams_that_fit_on_two_pages(int totalNumItems) {

    PageOfNumbers firstPage = new PageOfNumbers(totalNumItems);

    assertThat(firstPage.getPageContent())
        .hasSize(ITEMS_PER_PAGE)
        .containsExactlyElementsOf(getRangeAsList(0, ITEMS_PER_PAGE));

    PageOfNumbers secondPage = firstPage.getNextPage().get();

    assertThat(secondPage.getCurrentPageNumber())
        .isEqualTo(1);

    assertThat(secondPage.getPageContent())
        .hasSize(totalNumItems - ITEMS_PER_PAGE)
        .containsExactlyElementsOf(getRangeAsList(ITEMS_PER_PAGE, totalNumItems));

    assertThat(secondPage.getFirstPage())
        .isPresent()
        .get()
        .extracting(PageOfNumbers::getCurrentPageNumber)
        .isEqualTo(0);

    assertThat(secondPage.getPrevPage())
        .isPresent()
        .get()
        .extracting(PageOfNumbers::getCurrentPageNumber)
        .isEqualTo(0);

    assertThat(secondPage.getNextPage())
        .isNotPresent();

  }

  @ParameterizedTest
  @ValueSource(ints = { 2 * ITEMS_PER_PAGE + 1, 3 * ITEMS_PER_PAGE - 1, 3 * ITEMS_PER_PAGE, })
  void should_handle_streams_that_fit_on_three_pages(int totalNumItems) {

    PageOfNumbers firstPage = new PageOfNumbers(totalNumItems);

    assertThat(firstPage.getPageContent())
        .hasSize(ITEMS_PER_PAGE)
        .containsExactlyElementsOf(getRangeAsList(0, ITEMS_PER_PAGE));

    PageOfNumbers secondPage = firstPage.getNextPage().get();

    assertThat(secondPage.getPageContent())
        .hasSize(ITEMS_PER_PAGE)
        .containsExactlyElementsOf(getRangeAsList(ITEMS_PER_PAGE, 2 * ITEMS_PER_PAGE));

    PageOfNumbers thirdPage = secondPage.getNextPage().get();

    assertThat(thirdPage.getCurrentPageNumber())
        .isEqualTo(2);

    assertThat(thirdPage.getPageContent())
        .hasSize(totalNumItems - 2 * ITEMS_PER_PAGE)
        .containsExactlyElementsOf(getRangeAsList(2 * ITEMS_PER_PAGE, totalNumItems));

    assertThat(thirdPage.getFirstPage())
        .isPresent()
        .get()
        .extracting(PageOfNumbers::getCurrentPageNumber)
        .isEqualTo(0);

    assertThat(thirdPage.getPrevPage())
        .isPresent()
        .get()
        .extracting(PageOfNumbers::getCurrentPageNumber)
        .isEqualTo(1);

    assertThat(thirdPage.getNextPage())
        .isNotPresent();

  }

  @Test
  void should_fail_if_paging_methods_are_called_when_pageNumber_is_null() {

    PageOfNumbers pageTemplate = new PageOfNumbers(null, ImmutableList.of(1, 2, 3));

    assertThrows(HalApiDeveloperException.class, pageTemplate::getPageContent);

    assertThrows(HalApiDeveloperException.class, pageTemplate::getFirstPage);

    assertThrows(HalApiDeveloperException.class, pageTemplate::getPrevPage);

    assertThrows(HalApiDeveloperException.class, pageTemplate::getNextPage);
  }

  @Test
  void getMaxPageSize_should_return_value_give_in_constructor() {

    PageOfNumbers page = new PageOfNumbers(0);

    assertThat(page.getMaxPageSize())
        .isEqualTo(ITEMS_PER_PAGE);
  }
}
