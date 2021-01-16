/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.osgi.it.tests;

import static io.wcm.caravan.rhyme.osgi.it.TestEnvironmentConstants.SERVICE_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.base.Stopwatch;

import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.functions.Function;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.osgi.it.extensions.HalApiClientExtension;
import io.wcm.caravan.rhyme.osgi.it.extensions.WaitForServerStartupExtension;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.caching.EvenOddItemsResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemState;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.CollectionParametersImpl;

@ExtendWith({ WaitForServerStartupExtension.class, HalApiClientExtension.class })
public class CachingExamplesIT {

  private final ExamplesEntryPointResource entryPoint;

  public CachingExamplesIT(HalApiClient halApiClient) {
    this.entryPoint = halApiClient.getEntryPoint(SERVICE_ID, ExamplesEntryPointResource.class);
  }

  private List<ItemState> getItems(int numItems, int delayMs, Function<EvenOddItemsResource, SingleSource<ItemCollectionResource>> func) {

    CollectionParametersImpl parameters = new CollectionParametersImpl()
        .withNumItems(numItems)
        .withEmbedItems(false)
        .withDelayMs(delayMs);

    return entryPoint.getCachingExamples()
        .flatMap(caching -> caching.getEvenAndOddItems(parameters))
        .flatMap(func)
        .flatMapObservable(ItemCollectionResource::getItems)
        .flatMapSingle(ItemResource::getProperties)
        .toList().blockingGet();
  }

  private List<ItemState> getItemsWithoutDelay(int numItems, Function<EvenOddItemsResource, SingleSource<ItemCollectionResource>> func) {
    return getItems(numItems, 0, func);
  }

  private List<ItemState> getItemsWithDelay(int numItems, int delayMs, Function<EvenOddItemsResource, SingleSource<ItemCollectionResource>> func) {
    return getItems(numItems, delayMs, func);
  }

  @Test
  public void odd_items_should_be_correct() {

    List<ItemState> oddItems = getItemsWithoutDelay(5, EvenOddItemsResource::getOddItems);

    assertThat(oddItems).hasSize(2);
    assertThat(oddItems).extracting(s -> s.index).allMatch(i -> i % 2 == 1);
  }

  @Test
  public void even_items_should_be_correct() {

    List<ItemState> evenItems = getItemsWithoutDelay(5, EvenOddItemsResource::getEvenItems);

    assertThat(evenItems).hasSize(3);
    assertThat(evenItems).extracting(s -> s.index).allMatch(i -> i % 2 == 0);
  }

  @Test
  public void second_call_should_use_cached_items() {

    // measure the time it takes to load a collection of even resources, where each item takes 200ms to generate
    int delayMs = 200;
    Stopwatch uncachedStopwatch = Stopwatch.createStarted();
    getItemsWithDelay(10, delayMs, EvenOddItemsResource::getEvenItems);

    // because for the first request, these items cannot be cached the total time must be greater than the delay
    assertThat(uncachedStopwatch.elapsed(TimeUnit.MILLISECONDS)).isGreaterThan(delayMs);

    // now fetch a *smaller* collection with the same delay parameters
    Stopwatch cachedStopwatch = Stopwatch.createStarted();
    getItemsWithDelay(6, delayMs, EvenOddItemsResource::getEvenItems);

    // because the items should be served from cache, the total response times should be smaller than the delay
    assertThat(cachedStopwatch.elapsed(TimeUnit.MILLISECONDS)).isLessThan(delayMs);
  }
}
