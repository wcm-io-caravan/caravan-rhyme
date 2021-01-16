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
package io.wcm.caravan.rhyme.impl.client.blocking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.spi.JsonResourceLoader;
import io.wcm.caravan.ryhme.testing.ConversionFunctions;
import io.wcm.caravan.ryhme.testing.resources.TestResourceState;

/**
 * Variation of the tests in {@link io.wcm.caravan.rhyme.impl.client.ResourceStateTest}
 * for blocking HAL API interfaces (i.e. that are not using reactive return types for their methods)
 */
public class ResourceStateTest {

  private static final String RESOURCE_URL = "/";

  private RequestMetricsCollector metrics;
  private JsonResourceLoader jsonLoader;

  @BeforeEach
  public void setUp() {
    metrics = RequestMetricsCollector.create();
    jsonLoader = Mockito.mock(JsonResourceLoader.class);
  }

  private <T> T createClientProxy(Class<T> halApiInterface) {
    HalApiClient client = HalApiClient.create(jsonLoader, metrics);
    T clientProxy = client.getEntryPoint(RESOURCE_URL, halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }

  private void mockHalResponseWithSingle(Object state) {

    HalResource hal = new HalResource(state, RESOURCE_URL);

    when(jsonLoader.loadJsonResource(eq(RESOURCE_URL)))
        .thenReturn(Single.just(ConversionFunctions.toJsonResponse(hal)));
  }

  @HalApiInterface
  interface ResourceWithRequiredState {

    @ResourceState
    TestResourceState getProperties();
  }

  @Test
  public void required_resource_state_should_be_emitted() throws Exception {

    mockHalResponseWithSingle(new TestResourceState().withText("test"));

    TestResourceState properties = createClientProxy(ResourceWithRequiredState.class)
        .getProperties();

    assertThat(properties).isNotNull();
    assertThat(properties.text).isEqualTo("test");
  }


  @HalApiInterface
  interface ResourceWithOptionalState {

    @ResourceState
    Optional<TestResourceState> getProperties();
  }

  @Test
  public void optional_resource_state_should_be_emitted() throws Exception {

    mockHalResponseWithSingle(new TestResourceState().withText("test"));

    TestResourceState properties = createClientProxy(ResourceWithOptionalState.class)
        .getProperties().get();

    assertThat(properties).isNotNull();
    assertThat(properties.text).isEqualTo("test");
  }

  @Test
  public void optional_resource_state_should_be_empty_if_no_properties_are_set() throws Exception {

    mockHalResponseWithSingle(JsonNodeFactory.instance.objectNode());

    Optional<TestResourceState> optionalProperties = createClientProxy(ResourceWithOptionalState.class)
        .getProperties();

    assertThat(optionalProperties.isPresent()).isFalse();
  }
}
