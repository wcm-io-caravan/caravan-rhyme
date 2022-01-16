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
package io.wcm.caravan.rhyme.api.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.client.HalApiClientImpl;
import io.wcm.caravan.rhyme.impl.reflection.CompositeHalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;
import io.wcm.caravan.rhyme.testing.TestState;


public class HalApiClientTest {

  @Test
  void create_should_use_a_default_http_client_implementation() throws Exception {

    HalApiClient client = HalApiClient.create();

    Throwable ex = catchThrowable(() -> getTestResourceFromUnknownHost(client));

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class)
        .hasRootCauseInstanceOf(UnknownHostException.class);
  }

  private TestState getTestResourceFromUnknownHost(HalApiClient client) {

    return client
        .getRemoteResource("http://foo.bar", LinkableTestResource.class)
        // just getting the remote resource won't trigger the HTTP request,
        // we actually have to call a method on the proxy and subscribe
        .getState()
        .blockingGet();

  }

  @Test
  void metrics_from_deprecated_methods_should_be_used() throws Exception {

    int maxAge = 123;

    HalResourceLoader loader = new HalResourceLoader() {

      @Override
      public Single<HalResponse> getHalResource(String uri) {

        return Single.just(new HalResponse().withBody(new HalResource()).withMaxAge(123));
      }
    };

    RequestMetricsCollector metrics = RequestMetricsCollector.create();

    @SuppressWarnings("deprecation")
    HalApiClient client = HalApiClient.create(loader, metrics);

    Maybe<TestState> testState = client.getRemoteResource("/", LinkableTestResource.class).getState();

    assertThat(testState.isEmpty().blockingGet())
        .isTrue();

    assertThat(metrics.getResponseMaxAge())
        .isEqualTo(maxAge);
  }

  @Test
  void type_support_from_deprecated_method_should_be_used() throws Exception {

    HalResourceLoader loader = mock(HalResourceLoader.class);
    RequestMetricsCollector metrics = mock(RequestMetricsCollector.class);

    HalApiAnnotationSupport annotationSupport = mock(HalApiAnnotationSupport.class);
    HalApiReturnTypeSupport returnTypeSupport = mock(HalApiReturnTypeSupport.class);

    @SuppressWarnings("deprecation")
    HalApiClient client = HalApiClient.create(loader, metrics, annotationSupport, returnTypeSupport);

    HalApiTypeSupport typeSupport = ((HalApiClientImpl)client).getTypeSupport();
    assertThat(typeSupport)
        .isInstanceOf(CompositeHalApiTypeSupport.class);

    when(annotationSupport.isHalApiInterface(String.class)).thenReturn(true);
    assertThat(typeSupport.isHalApiInterface(String.class))
        .isTrue();

    when(returnTypeSupport.isProviderOfOptionalValue(String.class)).thenReturn(true);
    assertThat(typeSupport.isProviderOfOptionalValue(String.class))
        .isTrue();
  }

}
