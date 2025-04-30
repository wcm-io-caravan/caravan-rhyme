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
package io.wcm.caravan.rhyme.caravan.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.impl.JsonPipelineFactoryImpl;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.client.cache.CachingHalResourceLoader;

@ExtendWith(OsgiContextExtension.class)
@ExtendWith(MockitoExtension.class)
class CaravanHalApiClientImplTest {

  private static final String SERVICE_ID = "/serviceId";

  private final OsgiContext context = new OsgiContext();

  @Mock
  private CaravanHttpClient httpClient;

  @BeforeEach
  void setUp() {
    context.registerService(CaravanHttpClient.class, httpClient);
  }

  private CaravanHalApiClientImpl createAndActivateHalApiClient() {

    return context.registerInjectActivateService(new CaravanHalApiClientImpl());
  }

  private JsonPipelineFactory createAndActivateJsonPipelineFactory() {
    context.registerService(MetricRegistry.class, Mockito.mock(MetricRegistry.class));
    return context.registerInjectActivateService(new JsonPipelineFactoryImpl());
  }

  private ObjectNode mockOkHalResponse() {

    ObjectNode body = JsonNodeFactory.instance.objectNode().put("foo", "bar");

    CaravanHttpMockUtils.mockHttpResponse(httpClient, 200, body, Duration.ofSeconds(60));

    return body;
  }

  @Test
  void should_use_CachingHalResourceLoader_if_JsonPipeline_not_present_at_runtime() {

    CaravanHalApiClientImpl clientImpl = createAndActivateHalApiClient();

    HalResourceLoader resourceLoader = clientImpl.getOrCreateHalResourceLoader(SERVICE_ID);

    assertThat(resourceLoader)
        .isInstanceOf(CachingHalResourceLoader.class);
  }

  @Test
  void should_use_JsonPipelineResourceLoader_if_JsonPipeline_is_present_at_runtime() {

    createAndActivateJsonPipelineFactory();

    CaravanHalApiClientImpl clientImpl = createAndActivateHalApiClient();

    HalResourceLoader resourceLoader = clientImpl.getOrCreateHalResourceLoader(SERVICE_ID);

    assertThat(resourceLoader)
        .isInstanceOf(CaravanJsonPipelineResourceLoader.class);
  }

  @Test
  void should_fail_if_null_service_id_is_used() {

    CaravanHalApiClientImpl clientImpl = createAndActivateHalApiClient();

    Throwable ex = Assertions.catchThrowable(() -> clientImpl.getOrCreateHalResourceLoader(null));

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class);
  }

  private LinkableTestResource getEntryPoint(CaravanHalApiClientImpl clientImpl) {
    RequestMetricsCollector metrics = RequestMetricsCollector.create();
    return clientImpl.getEntryPoint(SERVICE_ID, "/", LinkableTestResource.class, metrics);
  }

  @Test
  void getEntryPoint_reuses_resource_loader_for_multiple_Requests() {

    CaravanHalApiClientImpl clientImpl = createAndActivateHalApiClient();

    mockOkHalResponse();

    LinkableTestResource resource = getEntryPoint(clientImpl);

    // no HTTP requests will be made before the state is accessed
    assertThat(resource).isNotNull();
    verifyNoInteractions(httpClient);

    // but when it is accessed, a single http client request will be executed
    assertThat(resource.getState()).isNotNull();
    verify(httpClient).execute(any());

    // a second request to the same resource should not trigger another request
    LinkableTestResource secondResource = getEntryPoint(clientImpl);
    assertThat(secondResource.getState()).isNotNull();
    verify(httpClient).execute(any());
  }

  @Test
  void should_use_configuration_that_caches_exceptions() {

    CaravanHalApiClientImpl clientImpl = createAndActivateHalApiClient();

    HalResourceLoader resourceLoader = clientImpl.getOrCreateHalResourceLoader(SERVICE_ID);

    CaravanHttpMockUtils.mockHttpResponse(httpClient, 500, "", Duration.ofSeconds(60));

    assertThat(resourceLoader)
        .isInstanceOf(CachingHalResourceLoader.class);

    Throwable ex1 = catchThrowable(() -> resourceLoader.getHalResource("/").blockingGet());

    assertThat(ex1.getCause())
        .hasMessageStartingWith("An HTTP response with status code 500 was retrieved");

    Throwable ex2 = catchThrowable(() -> resourceLoader.getHalResource("/").blockingGet());

    assertThat(ex2.getCause())
        .hasMessageStartingWith("An error response with status code 500 from a previous request was found in cache");

    verify(httpClient).execute(any());
  }

  @HalApiInterface
  public interface LinkableTestResource extends LinkableResource {

    @ResourceState
    ObjectNode getState();
  }

}
