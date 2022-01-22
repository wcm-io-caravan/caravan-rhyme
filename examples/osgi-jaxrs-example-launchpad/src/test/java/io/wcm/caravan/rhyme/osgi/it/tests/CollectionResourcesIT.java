package io.wcm.caravan.rhyme.osgi.it.tests;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.base.Stopwatch;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.osgi.it.IntegrationTestEnvironment;
import io.wcm.caravan.rhyme.osgi.it.extensions.WaitForServerStartupExtension;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionParameters;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemState;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.CollectionParametersBean;

@ExtendWith({ WaitForServerStartupExtension.class })
class CollectionResourcesIT {

  private final ExamplesEntryPointResource entryPoint = IntegrationTestEnvironment.createEntryPointProxy();

  private Single<ItemCollectionResource> getCollection(int numItems, boolean embedItems, int delayMs) {

    CollectionParameters params = new CollectionParametersBean()
        .withNumItems(numItems)
        .withEmbedItems(embedItems)
        .withDelayMs(delayMs);

    return entryPoint.getCollectionExamples()
        .flatMap(collections -> collections.getDelayedCollection(params));
  }

  private List<ItemState> getCollectionItems(int numItems, boolean embedItems, int delayMs) {

    List<ItemState> items = getCollection(numItems, embedItems, delayMs)
        .flatMapObservable(ItemCollectionResource::getItems)
        .concatMapSingle(ItemResource::getProperties)
        .toList().blockingGet();

    return items;
  }

  @Test
  void linked_items_can_be_fetched() {

    int numItems = 5;
    boolean embedItems = false;
    int delayMs = 0;

    List<ItemState> items = getCollectionItems(numItems, embedItems, delayMs);

    assertThat(items)
        .hasSize(numItems);
  }

  @Test
  void linked_items_are_in_correct_order() {

    int numItems = 50;
    boolean embedItems = false;
    int delayMs = 0;

    List<ItemState> items = getCollectionItems(numItems, embedItems, delayMs);

    for (int i = 0; i < numItems; i++) {
      assertThat(items.get(i).index)
          .isEqualTo(i);
    }
  }

  @Test
  void linked_items_are_properly_delayed() {

    int numItems = 1;
    boolean embedItems = false;
    int delayMs = 1000;

    Stopwatch stopwatch = Stopwatch.createStarted();
    getCollectionItems(numItems, embedItems, delayMs);
    long responseTime = stopwatch.elapsed(MILLISECONDS);

    assertThat(responseTime)
        .isGreaterThanOrEqualTo(delayMs)
        .isLessThan(2 * delayMs);
  }

  @Test
  void embedded_items_can_be_fetched() {

    int numItems = 20;
    boolean embedItems = true;
    int delayMs = 0;

    List<ItemState> items = getCollectionItems(numItems, embedItems, delayMs);

    assertThat(items)
        .hasSize(numItems);
  }

  @Test
  void embedded_items_are_in_correct_order() {

    int numItems = 50;
    boolean embedItems = true;
    int delayMs = 0;

    List<ItemState> items = getCollectionItems(numItems, embedItems, delayMs);

    for (int i = 0; i < numItems; i++) {
      assertThat(items.get(i).index)
          .isEqualTo(i);
    }
  }

  @Test
  void embedded_items_are_properly_delayed() {

    int numItems = 50;
    boolean embedItems = true;
    int delayMs = 1000;

    Stopwatch stopwatch = Stopwatch.createStarted();
    getCollectionItems(numItems, embedItems, delayMs);
    long responseTime = stopwatch.elapsed(MILLISECONDS);

    assertThat(responseTime)
        .isGreaterThanOrEqualTo(delayMs)
        .isLessThan(5 * delayMs);
  }

  @Test
  void individial_item_can_be_fetched_with_null_delayMs_and_has_correct_properties() {

    int index = 3;
    Integer delayMs = null;

    ItemState item = entryPoint.getCollectionExamples()
        .flatMap(examples -> examples.getDelayedItem(index, delayMs))
        .flatMap(ItemResource::getProperties)
        .blockingGet();

    assertThat(item.index)
        .isEqualTo(index);

    assertThat(item.title)
        .isEqualTo("The item with index " + index);
  }

  @Test
  void individial_item_is_properly_delayed() {

    int index = 3;
    int delayMs = 1000;

    Stopwatch stopwatch = Stopwatch.createStarted();
    ItemState item = entryPoint.getCollectionExamples()
        .flatMap(examples -> examples.getDelayedItem(index, delayMs))
        .flatMap(ItemResource::getProperties)
        .blockingGet();

    long responseTime = stopwatch.elapsed(MILLISECONDS);

    assertThat(responseTime)
        .isGreaterThanOrEqualTo(delayMs);

    assertThat(item.title)
        .isEqualTo("The item with index " + index + " delayed by " + delayMs + "ms");
  }
}
