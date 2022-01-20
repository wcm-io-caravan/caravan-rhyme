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
public class ClientCollectionResourcesIT {

  private final ExamplesEntryPointResource entryPoint = IntegrationTestEnvironment.createEntryPointProxy();

  private Single<ItemCollectionResource> getCollectionThroughClient(Integer numItems, Boolean embedItems, Integer delayMs) {

    CollectionParameters params = new CollectionParametersBean()
        .withNumItems(numItems)
        .withEmbedItems(embedItems)
        .withDelayMs(delayMs);

    return entryPoint.getCollectionExamples()
        .flatMap(collections -> collections.getCollectionThroughClient(params));
  }

  private List<ItemState> getCollectionItemsThroughClient(Integer numItems, Boolean embedItems, Integer delayMs) {

    List<ItemState> items = getCollectionThroughClient(numItems, embedItems, delayMs)
        .flatMapObservable(ItemCollectionResource::getItems)
        .concatMapSingle(ItemResource::getProperties)
        .toList().blockingGet();

    return items;
  }

  @Test
  void linked_items_can_be_fetched() {

    Integer numItems = 5;
    Boolean embedItems = false;
    Integer delayMs = null;

    List<ItemState> items = getCollectionItemsThroughClient(numItems, embedItems, delayMs);

    assertThat(items)
        .hasSize(numItems);
  }

  @Test
  void linked_items_are_in_correct_order() {

    Integer numItems = 50;
    Boolean embedItems = false;
    Integer delayMs = null;

    List<ItemState> items = getCollectionItemsThroughClient(numItems, embedItems, delayMs);

    for (int i = 0; i < numItems; i++) {
      assertThat(items.get(i).index)
          .isEqualTo(i);
    }
  }

  @Test
  void linked_items_are_properly_delayed() {

    Integer numItems = 1;
    Boolean embedItems = false;
    Integer delayMs = 2000;

    Stopwatch stopwatch = Stopwatch.createStarted();
    getCollectionItemsThroughClient(numItems, embedItems, delayMs);
    long responseTime = stopwatch.elapsed(MILLISECONDS);

    assertThat(responseTime)
        .isGreaterThanOrEqualTo(delayMs)
        .isLessThan(5 * delayMs);
  }

  @Test
  void linked_items_are_fetched_simultaneously() {

    Integer numItems = 50;
    Boolean embedItems = false;
    Integer delayMs = 2000;

    Stopwatch stopwatch = Stopwatch.createStarted();
    getCollectionItemsThroughClient(numItems, embedItems, delayMs);
    long responseTime = stopwatch.elapsed(MILLISECONDS);

    assertThat(responseTime)
        .isGreaterThanOrEqualTo(delayMs)
        .isLessThan(numItems * (long)delayMs);
  }

  @Test
  void embedded_items_can_be_fetched() {

    Integer numItems = 20;
    Boolean embedItems = true;
    Integer delayMs = null;

    List<ItemState> items = getCollectionItemsThroughClient(numItems, embedItems, delayMs);

    assertThat(items)
        .hasSize(numItems);
  }

  @Test
  void embedded_items_are_in_correct_order() {

    Integer numItems = 50;
    Boolean embedItems = true;
    Integer delayMs = null;

    List<ItemState> items = getCollectionItemsThroughClient(numItems, embedItems, delayMs);

    for (int i = 0; i < numItems; i++) {
      assertThat(items.get(i).index)
          .isEqualTo(i);
    }
  }

  @Test
  void embedded_items_are_properly_delayed() {

    Integer numItems = 50;
    Boolean embedItems = true;
    Integer delayMs = 2000;

    Stopwatch stopwatch = Stopwatch.createStarted();
    getCollectionItemsThroughClient(numItems, embedItems, delayMs);
    long responseTime = stopwatch.elapsed(MILLISECONDS);

    assertThat(responseTime)
        .isGreaterThanOrEqualTo(delayMs)
        .isLessThan(5 * delayMs);
  }
}
