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

import static io.wcm.caravan.rhyme.microbenchmark.resources.ResourceParameters.NUM_EMBEDDED_RESOURCES;
import static io.wcm.caravan.rhyme.microbenchmark.resources.ResourceParameters.NUM_LINKED_RESOURCES;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openjdk.jmh.annotations.Mode.AverageTime;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.microbenchmark.RhymeState.MetricsToggle;
import io.wcm.caravan.rhyme.microbenchmark.resources.BenchmarkResourceState;
import io.wcm.caravan.rhyme.microbenchmark.resources.EmbeddableBenchmarkResource;
import io.wcm.caravan.rhyme.microbenchmark.resources.LinkableBenchmarkResource;

@BenchmarkMode(AverageTime)
@OutputTimeUnit(MILLISECONDS)
@Fork(value = 2, warmups = 0)
@Warmup(iterations = 4, time = 2, timeUnit = SECONDS)
@Measurement(iterations = 4, time = 2, timeUnit = SECONDS)
@State(Scope.Benchmark)
public class ClientBenchmarks {

  private final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final JsonFactory jsonFactory = new JsonFactory(objectMapper);

  private ImmutableList<Object> callClientMethods(RhymeState state, HalResourceLoader loader, MetricsToggle metrics) {

    Rhyme rhyme = state.createRhyme(loader, metrics);

    LinkableBenchmarkResource clientProxy = rhyme.getRemoteResource("/", LinkableBenchmarkResource.class);

    return ImmutableList.of(
        clientProxy.getState().blockingGet(),

        clientProxy.createLink(),

        clientProxy.getEmbedded1().flatMapSingle(EmbeddableBenchmarkResource::getState).map(BenchmarkResourceState::getBar).toList().blockingGet(),

        clientProxy.getLinked1().flatMapSingle(LinkableBenchmarkResource::getState).map(BenchmarkResourceState::getBar).toList().blockingGet(),

        clientProxy.getLinked1().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked2().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked3().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked4().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked5().map(l -> l.createLink().getHref()).toList().blockingGet());
  }

  @Benchmark
  public List<Object> withoutOverhead(RhymeState state, ResourceLoaders loaders) {
    return callClientMethods(state, loaders.preBuilt, MetricsToggle.DISABLED);
  }

  @Benchmark
  public List<Object> withMetrics(RhymeState state, ResourceLoaders loaders) {
    return callClientMethods(state, loaders.preBuilt, MetricsToggle.ENABLED);
  }

  @Benchmark
  public List<Object> withParsing(RhymeState state, ResourceLoaders loaders) {
    return callClientMethods(state, loaders.parsing, MetricsToggle.DISABLED);
  }

  @Benchmark
  public List<Object> withNetworkAndParsing(RhymeState state, ResourceLoaders loaders) {
    return callClientMethods(state, loaders.network, MetricsToggle.DISABLED);
  }

  @Benchmark
  public List<Object> withCaching(RhymeState state, ResourceLoaders loaders) {
    return callClientMethods(state, loaders.caching, MetricsToggle.DISABLED);
  }

  @Benchmark
  public List<ObjectNode> onlyParsing(ResourceLoaders loaders) throws IOException {

    List<ObjectNode> list = new ArrayList<>();
    for (int i = 0; i < NUM_LINKED_RESOURCES + 1; i++) {
      try (JsonParser parser = jsonFactory.createParser(loaders.getFirstResponseBytes())) {
        list.add(parser.readValueAsTree());
      }
    }
    return list;
  }

  @Benchmark
  public List<String> onlyNetwork(ResourceLoaders loaders) throws IOException {

    List<String> list = new ArrayList<>();
    for (int i = 0; i < NUM_LINKED_RESOURCES + 1; i++) {
      list.add(IOUtils.toString(URI.create("http://localhost:" + loaders.getNettyPort() + "/")));
    }
    return list;
  }

  @Benchmark
  public List<BenchmarkResourceState> onlyMapping(ResourceLoaders loaders) {

    List<BenchmarkResourceState> list = new ArrayList<>();
    for (int i = 0; i < NUM_LINKED_RESOURCES + NUM_EMBEDDED_RESOURCES + 1; i++) {
      list.add(objectMapper.convertValue(loaders.getFirstResponseJson(), BenchmarkResourceState.class));
    }
    return list;
  }
}
