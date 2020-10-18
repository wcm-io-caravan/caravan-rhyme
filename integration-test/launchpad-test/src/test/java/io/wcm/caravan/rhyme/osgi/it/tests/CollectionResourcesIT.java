package io.wcm.caravan.rhyme.osgi.it.tests;

import static io.wcm.caravan.rhyme.osgi.it.TestEnvironmentConstants.SERVICE_ID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.base.Stopwatch;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.osgi.it.extensions.HalApiClientExtension;
import io.wcm.caravan.rhyme.osgi.it.extensions.WaitForServerStartupExtension;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionParameters;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemState;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.CollectionParametersImpl;

@ExtendWith({ WaitForServerStartupExtension.class, HalApiClientExtension.class })
public class CollectionResourcesIT {

  private final ExamplesEntryPointResource entryPoint;

  public CollectionResourcesIT(HalApiClient halApiClient) {
    this.entryPoint = halApiClient.getEntryPoint(SERVICE_ID, ExamplesEntryPointResource.class);
  }

  private Single<ItemCollectionResource> getCollection(Integer numItems, Boolean embedItems, Integer delayMs) {

    CollectionParameters params = new CollectionParametersImpl()
        .withNumItems(numItems)
        .withEmbedItems(embedItems)
        .withDelayMs(delayMs);

    return entryPoint.getCollectionExamples()
        .flatMap(collections -> collections.getCollection(params));
  }

  private List<ItemState> getCollectionItems(Integer numItems, Boolean embedItems, Integer delayMs) {

    List<ItemState> items = getCollection(numItems, embedItems, delayMs)
        .flatMapObservable(ItemCollectionResource::getItems)
        .concatMapSingle(ItemResource::getProperties)
        .toList().blockingGet();

    return items;
  }

  @Test
  public void linked_items_can_be_fetched() {

    Integer numItems = 5;
    Boolean embedItems = false;
    Integer delayMs = null;

    List<ItemState> items = getCollectionItems(numItems, embedItems, delayMs);

    assertThat(items).hasSize(numItems);
  }

  @Test
  public void linked_items_are_in_correct_order() {

    Integer numItems = 50;
    Boolean embedItems = false;
    Integer delayMs = null;

    List<ItemState> items = getCollectionItems(numItems, embedItems, delayMs);

    for (int i = 0; i < numItems; i++) {
      assertThat(items.get(i).index).isEqualTo(i);
    }
  }

  @Test
  public void linked_items_are_properly_delayed() {

    Integer numItems = 1;
    Boolean embedItems = false;
    Integer delayMs = 1000;

    Stopwatch stopwatch = Stopwatch.createStarted();
    getCollectionItems(numItems, embedItems, delayMs);
    long responseTime = stopwatch.elapsed(MILLISECONDS);

    assertThat(responseTime).isGreaterThanOrEqualTo(delayMs);
    assertThat(responseTime).isLessThan(2 * delayMs);
  }

  @Test
  public void embedded_items_can_be_fetched() {

    Integer numItems = 20;
    Boolean embedItems = true;
    Integer delayMs = null;

    List<ItemState> items = getCollectionItems(numItems, embedItems, delayMs);

    assertThat(items).hasSize(numItems);
  }

  @Test
  public void embedded_items_are_in_correct_order() {

    Integer numItems = 50;
    Boolean embedItems = true;
    Integer delayMs = null;

    List<ItemState> items = getCollectionItems(numItems, embedItems, delayMs);

    for (int i = 0; i < numItems; i++) {
      assertThat(items.get(i).index).isEqualTo(i);
    }
  }

  @Test
  public void embedded_items_are_properly_delayed() {

    Integer numItems = 50;
    Boolean embedItems = true;
    Integer delayMs = 1000;

    Stopwatch stopwatch = Stopwatch.createStarted();
    getCollectionItems(numItems, embedItems, delayMs);
    long responseTime = stopwatch.elapsed(MILLISECONDS);

    assertThat(responseTime).isGreaterThanOrEqualTo(delayMs);
    assertThat(responseTime).isLessThan(2 * delayMs);
  }
}
