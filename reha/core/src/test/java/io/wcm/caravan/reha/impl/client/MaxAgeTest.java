/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.reha.impl.client;

import static io.wcm.caravan.reha.api.relations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.impl.client.ClientTestSupport.ResourceTreeClientTestSupport;
import io.wcm.caravan.reha.testing.TestState;
import io.wcm.caravan.reha.testing.resources.TestResource;

public class MaxAgeTest {

  private final ResourceTreeClientTestSupport client = ClientTestSupport.withResourceTree();
  private final TestResource entryPoint = client.getEntryPoint();
  private final RequestMetricsCollector metrics = client.getMetrics();

  @HalApiInterface
  interface EntryPoint {

    @ResourceState
    Maybe<TestState> getState();

    @RelatedResource(relation = ITEM)
    Observable<LinkedResource> getLinked();

  }

  @HalApiInterface
  interface LinkedResource {

    @ResourceState
    Maybe<TestState> getState();
  }

  private void loadEntryPoint() {

    client.createProxy(EntryPoint.class)
        .getState()
        .blockingGet();
  }

  @Test
  public void max_age_should_be_null_if_nothing_was_specified() {

    loadEntryPoint();

    assertThat(metrics.getResponseMaxAge()).isNull();
  }

  @Test
  public void explicit_max_age_should_be_used_if_no_headers_found_in_response() {

    metrics.setResponseMaxAge(Duration.ofSeconds(45));

    loadEntryPoint();

    assertThat(metrics.getResponseMaxAge()).isEqualTo(45);
  }

  @Test
  public void max_age_from_entrypoint_response_should_be_used_if_no_explicit_value_defined() {

    entryPoint.withMaxAge(55);

    loadEntryPoint();

    assertThat(metrics.getResponseMaxAge()).isEqualTo(55);
  }

  @Test
  public void max_age_from_entrypoint_response_should_be_used_if_smaller_than_explicit_value() {

    metrics.setResponseMaxAge(Duration.ofSeconds(125));
    entryPoint.withMaxAge(55);

    loadEntryPoint();

    assertThat(metrics.getResponseMaxAge()).isEqualTo(55);
  }

  @Test
  public void explicit_max_age_should_be_used_if_smaller_than_header_from_entry_point() {

    metrics.setResponseMaxAge(Duration.ofSeconds(45));
    entryPoint.withMaxAge(180);

    loadEntryPoint();

    assertThat(metrics.getResponseMaxAge()).isEqualTo(45);
  }

  @Test
  public void smallest_max_age_from_all_loaded_resources_should_be_used() {

    entryPoint.withMaxAge(55);
    entryPoint.createLinked(ITEM).withMaxAge(15);
    entryPoint.createLinked(ITEM).withMaxAge(85);

    client.createProxy(EntryPoint.class)
        .getLinked().flatMapMaybe(LinkedResource::getState)
        .toList().blockingGet();

    assertThat(metrics.getResponseMaxAge()).isEqualTo(15);
  }

  @Test
  public void resources_without_max_age_header_should_be_ignored() {

    entryPoint.withMaxAge(55);
    entryPoint.createLinked(ITEM);
    entryPoint.createLinked(ITEM).withMaxAge(85);

    client.createProxy(EntryPoint.class)
        .getLinked().flatMapMaybe(LinkedResource::getState)
        .toList().blockingGet();

    assertThat(metrics.getResponseMaxAge()).isEqualTo(55);
  }
}
