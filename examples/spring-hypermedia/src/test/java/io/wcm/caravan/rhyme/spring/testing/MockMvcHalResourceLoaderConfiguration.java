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
package io.wcm.caravan.rhyme.spring.testing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.collect.LinkedHashMultimap;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

/**
 * An implementation of {@link HalResourceLoader} for integration-tests that is using the {@link MockMvc} class
 * to fetch resources from the currently running {@link WebApplicationContext}. It allows you to create
 * a {@link HalApiClient} to navigate through your resources running in your application the same way as an external
 * client would. These tests can cover all of the following aspects with very little code:
 * <ul>
 * <li>the calls to the controller methods that create your server-side resource implementations</li>
 * <li>the rendering / serialization of these resources to the HAL+JSON format</li>
 * <li>parsing these responses as HAL+JSON with the {@link HalApiClient}</li>
 * <li>expanding link templates and following links to other resources</li>
 * </ul>
 */
@TestConfiguration
public class MockMvcHalResourceLoaderConfiguration {

  private final HalResourceLoader delegate;

  public MockMvcHalResourceLoaderConfiguration(@Autowired WebApplicationContext applicationContext) {

    MockMvcClient mockMvcClient = new MockMvcClient(applicationContext);

    delegate = HalResourceLoaderBuilder.create().withCustomHttpClient(mockMvcClient).build();
  }

  public HalResponse getResponse(String uri) {

    return getResourceLoader().getHalResource(uri).blockingGet();
  }

  @Primary
  @Bean
  public HalResourceLoader getResourceLoader() {

    return delegate;
  }

  private static final class MockMvcClient implements HttpClientSupport {

    private final MockMvc mockMvc;

    private MockMvcClient(WebApplicationContext applicationContext) {

      this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Override
    public void executeGetRequest(URI uri, HttpClientCallback callback) {
      try {
        MvcResult result = mockMvc.perform(get(uri)).andReturn();

        MockHttpServletResponse response = result.getResponse();

        callback.onHeadersAvailable(response.getStatus(), createHeadersMap(response));

        callback.onBodyAvailable(new ByteArrayInputStream(response.getContentAsByteArray()));
      }
      catch (Exception e) {
        callback.onExceptionCaught(e);
      }
    }

    private Map<String, Collection<String>> createHeadersMap(MockHttpServletResponse response) {

      LinkedHashMultimap<String, String> headers = LinkedHashMultimap.create();

      response.getHeaderNames().forEach(name -> headers.putAll(name, response.getHeaders(name)));

      return headers.asMap();
    }
  }
}
