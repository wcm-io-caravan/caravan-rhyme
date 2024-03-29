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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Stopwatch;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;

@ExtendWith(MockitoExtension.class)
class EmissionStopwatchTest {

  private static final String METHOD_DESC = "test";
  @Mock
  private RequestMetricsCollector metrics;

  @Mock
  private RequestMetricsStopwatch stopwatch;

  private static long sleepAndGetTimeSlept() throws InterruptedException {
    Stopwatch sw = Stopwatch.createStarted();
    Thread.sleep(20);
    return sw.elapsed(TimeUnit.MICROSECONDS);
  }

  @BeforeEach
  public void setUp() {

    when(metrics.startStopwatch(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(new RequestMetricsStopwatch() {

          Stopwatch sw = Stopwatch.createStarted();

          @Override
          public void close() {

            metrics.onMethodInvocationFinished(EmissionStopwatch.class, METHOD_DESC, sw.elapsed(TimeUnit.MICROSECONDS));
          }
        });
  }

  @Test
  void should_measure_emission_times_for_single() throws InterruptedException {

    SingleSubject<String> subject = SingleSubject.create();

    subject.compose(EmissionStopwatch.collectMetrics(() -> METHOD_DESC, metrics))
        .subscribe();

    long microsSlept = sleepAndGetTimeSlept();

    subject.onSuccess("item");

    Mockito.verify(metrics).onMethodInvocationFinished(eq(EmissionStopwatch.class), eq(METHOD_DESC), longThat(micros -> micros >= microsSlept));
  }

  @Test
  void should_measure_emission_times_for_observable() throws InterruptedException {

    Subject<String> subject = PublishSubject.create();

    subject.compose(EmissionStopwatch.collectMetrics(() -> METHOD_DESC, metrics))
        .subscribe();

    long microsSlept = sleepAndGetTimeSlept();

    subject.onNext("item1");
    subject.onComplete();

    Mockito.verify(metrics).onMethodInvocationFinished(eq(EmissionStopwatch.class), eq(METHOD_DESC), longThat(micros -> micros >= microsSlept));
  }

  @Test
  void should_allow_multiple_subscribers_for_observable() throws InterruptedException {

    SingleSubject<String> subject = SingleSubject.create();

    Single<String> obs = subject.compose(EmissionStopwatch.collectMetrics(() -> METHOD_DESC, metrics));
    obs.subscribe();
    obs.subscribe();

    long microsSlept = sleepAndGetTimeSlept();

    subject.onSuccess("item");

    Mockito.verify(metrics, times(2)).onMethodInvocationFinished(eq(EmissionStopwatch.class), eq(METHOD_DESC), longThat(micros -> micros >= microsSlept));
  }

  @Test
  void should_not_fail_if_emissions_come_before_subscriptions() {

    Subject<String> subject = PublishSubject.create();

    Observable<String> obs = subject.compose(EmissionStopwatch.collectMetrics(() -> METHOD_DESC, metrics));

    subject.onNext("item1");
    subject.onComplete();

    obs.subscribe();

    Mockito.verify(metrics).onMethodInvocationFinished(eq(EmissionStopwatch.class), eq(METHOD_DESC), ArgumentMatchers.anyLong());
  }
}
