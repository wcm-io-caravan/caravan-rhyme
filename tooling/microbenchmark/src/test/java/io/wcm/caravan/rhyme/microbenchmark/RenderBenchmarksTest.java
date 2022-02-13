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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.api.common.HalResponse;

class RenderBenchmarksTest {

  private final RhymeState state = new RhymeState();
  private final ResourceLoaders loaders = new ResourceLoaders();

  private final RenderBenchmarks benchmarks = new RenderBenchmarks();

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

    HalResponse response = benchmarks.withoutOverhead(state);

    assertThat(response.getStatus())
        .isEqualTo(200);
  }

  @Test
  void withInstanceCreation() {

    HalResponse response = benchmarks.withInstanceCreation(state);

    assertThat(response.getStatus())
        .isEqualTo(200);
  }

  @Test
  void withMetrics() {

    HalResponse response = benchmarks.withMetrics(state);

    assertThat(response.getStatus())
        .isEqualTo(200);
  }

  @Test
  void onlyObjectMapping() {

    assertThat(benchmarks.onlyObjectMapping(loaders))
        .isNotEmpty();
  }
}
