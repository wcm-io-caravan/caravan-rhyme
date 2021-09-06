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
package io.wcm.caravan.rhyme.impl.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.SingleSubject;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.testing.ConversionFunctions;
import io.wcm.caravan.rhyme.testing.resources.TestResource;
import io.wcm.caravan.rhyme.testing.resources.TestResourceTree;

public class ClientTestSupport {

  static final String ENTRY_POINT_URI = "/";

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();

  protected final HalResourceLoader jsonLoader;

  protected final TestResourceTree testResourceTree;

  protected ClientTestSupport(HalResourceLoader jsonLoader) {
    this.jsonLoader = jsonLoader;
    this.testResourceTree = null;
  }

  protected ClientTestSupport(TestResourceTree testResourceTree) {
    this.jsonLoader = testResourceTree;
    this.testResourceTree = testResourceTree;
  }

  <T> T createProxy(Class<T> halApiInterface) {
    HalApiClient client = getHalApiClient();
    T clientProxy = client.getRemoteResource(ENTRY_POINT_URI, halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }

  HalApiClient getHalApiClient() {
    return HalApiClient.create(jsonLoader, metrics);
  }

  RequestMetricsCollector getMetrics() {
    return metrics;
  }

  static ResourceTreeClientTestSupport withResourceTree() {
    return new ResourceTreeClientTestSupport();
  }

  public static MockClientTestSupport withMocking() {
    return new MockClientTestSupport();
  }

  static class ResourceTreeClientTestSupport extends ClientTestSupport {

    ResourceTreeClientTestSupport() {
      super(new TestResourceTree());
    }

    TestResource getEntryPoint() {
      return testResourceTree.getEntryPoint();
    }
  }

  public static class MockClientTestSupport extends ClientTestSupport {


    MockClientTestSupport() {
      super(Mockito.mock(HalResourceLoader.class));
    }

    public HalResourceLoader getMockJsonLoader() {
      return this.jsonLoader;
    }

    public void mockResponseWithSupplier(String uri, Supplier<Single<HalResponse>> supplier) {

      when(jsonLoader.getHalResource(uri))
          .thenAnswer(new Answer<Single<HalResponse>>() {

            @Override
            public Single<HalResponse> answer(InvocationOnMock invocation) throws Throwable {
              return supplier.get();
            }
          });
    }

    public SubscriberCounter mockResponseWithSingle(String uri, Single<HalResponse> value) {

      SubscriberCounter counter = new SubscriberCounter(value);

      when(jsonLoader.getHalResource(uri))
          .thenReturn(counter.getCountingSingle());

      return counter;
    }

    public SubscriberCounter mockFailedResponse(String uri, Integer statusCode) {

      HalApiClientException hace = new HalApiClientException("Simulated failed response", statusCode, uri, null);

      return mockResponseWithSingle(uri, Single.error(hace));
    }

    public SingleSubject<HalResource> mockHalResponseWithSubject(String uri) {

      SingleSubject<HalResource> testSubject = SingleSubject.create();

      mockResponseWithSingle(uri, testSubject.map(ConversionFunctions::toJsonResponse));

      return testSubject;
    }

    public SubscriberCounter mockHalResponse(String uri, HalResource hal) {

      HalResponse response = ConversionFunctions.toJsonResponse(hal);

      return mockResponseWithSingle(uri, Single.just(response));
    }

    public SubscriberCounter mockHalResponseWithState(String uri, Object state) {

      HalResource hal = new HalResource(state, uri);

      return mockHalResponse(uri, hal);
    }


    public static final class SubscriberCounter {

      private final AtomicInteger counter = new AtomicInteger();

      private final Single<HalResponse> countingSingle;

      private SubscriberCounter(Single<HalResponse> delegate) {
        // for the test case that returns a null single on purpose, we must not use defer to create a different single
        if (delegate == null) {
          countingSingle = null;
        }
        else {
          countingSingle = Single.defer(() -> {
            counter.incrementAndGet();

            return delegate;
          });
        }
      }

      public Single<HalResponse> getCountingSingle() {
        return countingSingle;
      }

      public int getCount() {
        return counter.get();
      }
    }
  }
}
