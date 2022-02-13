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

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.microbenchmark.RhymeState.Metrics;
import io.wcm.caravan.rhyme.microbenchmark.resources.DynamicResourceImpl;
import io.wcm.caravan.rhyme.microbenchmark.resources.ResourceParameters;
import io.wcm.caravan.rhyme.microbenchmark.resources.StaticResourceImpl;

@BenchmarkMode(AverageTime)
@OutputTimeUnit(MILLISECONDS)
@Fork(value = 2, warmups = 0)
@Warmup(iterations = 2, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = SECONDS)
@State(Scope.Benchmark)
public class RenderBenchmarks {

  private final ObjectMapper mapper = new ObjectMapper();

  HalResponse render(RhymeState state, LinkableResource resource, Metrics metrics) {

    Rhyme rhyme = state.createRhyme(HalResourceLoader.create(), metrics);

    return rhyme.renderResponse(resource).blockingGet();
  }

  @Benchmark
  public HalResponse withoutOverhead(RhymeState state) {

    return render(state, new StaticResourceImpl("/"), Metrics.DISABLED);
  }

  @Benchmark
  public HalResponse withMetrics(RhymeState state) {

    return render(state, new StaticResourceImpl("/"), Metrics.ENABLED);
  }

  @Benchmark
  public HalResponse withInstanceCreation(RhymeState state) {

    return render(state, new DynamicResourceImpl("/"), Metrics.DISABLED);
  }

  @Benchmark
  public List<ObjectNode> onlyObjectMapping(RhymeState state) {

    List<ObjectNode> list = new ArrayList<>();
    for (int i = 0; i < ResourceParameters.NUM_EMBEDDED_RESOURCES + 1; i++) {
      list.add(mapper.convertValue(state.getFirstResponseState(), ObjectNode.class));
    }
    return list;
  }
}
