package io.wcm.caravan.rhyme.microbenchmark;
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientBenchmarksTest {

  private final RhymeState state = new RhymeState();
  private final ResourceLoaders loaders = new ResourceLoaders();

  private final ClientBenchmarks benchmarks = new ClientBenchmarks();

  @BeforeEach
  void init() {
    loaders.init();
  }

  @AfterEach
  void tearDown() {
    loaders.tearDown();
    state.tearDown();
  }

  @Test
  void withoutOverhead() {

    assertThat(benchmarks.withoutOverhead(state, loaders))
        .isNotEmpty();
  }

  @Test
  void withMetrics() {

    assertThat(benchmarks.withMetrics(state, loaders))
        .isNotEmpty();
  }

  @Test
  void withParsing() {

    assertThat(benchmarks.withParsing(state, loaders))
        .isNotEmpty();
  }

  @Test
  void withCaching() {

    assertThat(benchmarks.withCaching(state, loaders))
        .isNotEmpty();
  }

  @Test
  void withNetworkAndParsing() {

    assertThat(benchmarks.withNetworkAndParsing(state, loaders))
        .isNotEmpty();
  }

  @Test
  void onlyParsing() throws IOException {

    assertThat(benchmarks.onlyParsing(loaders))
        .isNotEmpty()
        .allMatch(json -> json.has("foo"));
  }

  @Test
  void onlyMapping() throws IOException {

    assertThat(benchmarks.onlyMapping(loaders))
        .isNotEmpty()
        .allMatch(state -> state.getBar() != null);
  }

  @Test
  void onlyNetwork() throws IOException {

    assertThat(benchmarks.onlyNetwork(loaders))
        .isNotEmpty()
        .allMatch(body -> body.startsWith("{"));
  }
}
