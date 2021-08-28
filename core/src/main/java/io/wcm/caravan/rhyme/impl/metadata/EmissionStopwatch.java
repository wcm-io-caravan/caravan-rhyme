/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
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
package io.wcm.caravan.rhyme.impl.metadata;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.core.SingleTransformer;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;

/**
 * A transformer that measures the time between subscription and completion of any {@link Single} or {@link Observable}
 * @param <T> type of the emitted objects
 */
public class EmissionStopwatch<T> implements SingleTransformer<T, T>, ObservableTransformer<T, T> {

  private final RequestMetricsCollector metrics;
  private final Supplier<String> message;
  private final AtomicInteger itemCounter = new AtomicInteger();

  private RequestMetricsStopwatch stopwatch;

  EmissionStopwatch(RequestMetricsCollector metrics, Supplier<String> message) {
    this.metrics = metrics;
    this.message = message;
  }

  /**
   * @param message describes the task that was executed
   * @param metrics to collect the emisison times
   * @return a Transformer to use with {@link Single#compose(SingleTransformer)} or
   *         {@link Observable#compose(ObservableTransformer)}
   */
  public static <T> EmissionStopwatch<T> collectMetrics(Supplier<String> message, RequestMetricsCollector metrics) {
    return new EmissionStopwatch<T>(metrics, message);
  }

  @Override
  public SingleSource<T> apply(Single<T> upstream) {

    return upstream
        .doOnSubscribe(d -> startStopwatch())
        .doOnSuccess(o -> sendMetrics());
  }

  @Override
  public ObservableSource<T> apply(Observable<T> upstream) {

    return upstream
        .doOnSubscribe(d -> startStopwatch())
        .doOnNext(i -> itemCounter.incrementAndGet())
        .doOnTerminate(this::sendMetrics);
  }


  private void startStopwatch() {

    if (stopwatch == null) {
      stopwatch = metrics.startStopwatch(EmissionStopwatch.class, message);
    }
  }

  private void sendMetrics() {

    if (stopwatch != null) {
      stopwatch.close();
    }
  }

}
