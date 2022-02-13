/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
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
package io.wcm.caravan.rhyme.microbenchmark;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openjdk.jmh.annotations.Mode.AverageTime;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.microbenchmark.RhymeState.Metrics;

@BenchmarkMode(AverageTime)
@OutputTimeUnit(MILLISECONDS)
@Fork(value = 2, warmups = 0)
@Warmup(iterations = 4, time = 2, timeUnit = SECONDS)
@Measurement(iterations = 4, time = 2, timeUnit = SECONDS)
@State(Scope.Benchmark)
public class ClientBenchmarks {

  private ImmutableList<Object> callClientMethods(RhymeState state, HalResourceLoader loader, Metrics metrics) {

    Rhyme rhyme = state.createRhyme(loader, metrics);

    LinkableBenchmarkResource clientProxy = rhyme.getRemoteResource("/", LinkableBenchmarkResource.class);

    return ImmutableList.of(
        clientProxy.getState().blockingGet(),

        clientProxy.createLink(),

        clientProxy.getEmbedded1().flatMapSingle(EmbeddableBenchmarkResource::getState).toList().blockingGet(),

        clientProxy.getLinked1().flatMapSingle(LinkableBenchmarkResource::getState).toList().blockingGet(),

        clientProxy.getLinked1().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked2().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked3().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked4().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked5().map(l -> l.createLink().getHref()).toList().blockingGet());
  }

  @Benchmark
  public Object withoutOverhead(RhymeState state) {
    return callClientMethods(state, state.preBuiltLoader, Metrics.DISABLED);
  }

  @Benchmark
  public Object withMetrics(RhymeState state) {
    return callClientMethods(state, state.preBuiltLoader, Metrics.ENABLED);
  }

  @Benchmark
  public Object withParsing(RhymeState state) {
    return callClientMethods(state, state.parsingLoader, Metrics.DISABLED);
  }

  @Benchmark
  public Object withNetwork(RhymeState state) {
    return callClientMethods(state, state.networkLoader, Metrics.DISABLED);
  }

  @Benchmark
  public Object withCachingO(RhymeState state) {
    return callClientMethods(state, state.cachingLoader, Metrics.DISABLED);
  }
}
