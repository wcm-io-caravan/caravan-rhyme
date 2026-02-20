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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalApiClientBuilder;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.rhyme.testing.resources.TestResourceState;

/**
 * Tests that verify the URL template stripping behavior in
 * {@link io.wcm.caravan.rhyme.impl.client.proxy.HalApiClientProxyFactory#validateUrlAndLoadResourceBody}
 * when a templated URI is used as the entry point URL.
 */
class TemplatedResourceLoadingTest {

  @HalApiInterface
  interface SimpleResource {

    @ResourceState
    Single<TestResourceState> getProperties();
  }

  private final MockClientTestSupport client = ClientTestSupport.withMocking();

  private void loadResourceAndVerifyRequestedUrl(String entryPointUri, String expectedLoadedUrl) {

    client.mockHalResponseWithState(expectedLoadedUrl, new TestResourceState());

    HalApiClient halApiClient = HalApiClientBuilder.create()
        .withResourceLoader(client.getMockJsonLoader())
        .build();

    halApiClient.getRemoteResource(entryPointUri, SimpleResource.class)
        .getProperties()
        .blockingGet();

    verify(client.getMockJsonLoader()).getHalResource(expectedLoadedUrl);
  }

  @Test
  void should_load_non_templated_url_without_modification() {

    loadResourceAndVerifyRequestedUrl("/items", "/items");
  }

  @Test
  void should_strip_query_template_when_loading_resource() {

    loadResourceAndVerifyRequestedUrl("/items{?page}", "/items");
  }

  @Test
  void should_strip_path_template_when_loading_resource() {

    loadResourceAndVerifyRequestedUrl("/items/{id}", "/items/");
  }

  @Test
  void should_strip_mixed_template_expressions() {

    loadResourceAndVerifyRequestedUrl("/items/{id}{?query}", "/items/");
  }

  @Test
  void should_strip_plus_template_when_loading_resource() {

    loadResourceAndVerifyRequestedUrl("/{+path}.rhyme", "/.rhyme");
  }
}
