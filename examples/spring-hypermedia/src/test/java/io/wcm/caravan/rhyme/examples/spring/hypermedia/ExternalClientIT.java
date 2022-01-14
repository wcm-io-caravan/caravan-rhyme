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
package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.testing.client.HalCrawler;
import io.wcm.caravan.rhyme.testing.spring.MockMvcHalResourceLoaderConfiguration;

/**
 * This test is similar to {@link MockMvcClientIT}, but it's starting
 * the Spring boot application so that it actually listens on a random server port,
 * and the resources under test are loaded via HTTP exactly as a real consumer would do.
 * Differences in behaviour to {@link MockMvcClientIT} are:
 * <ul>
 * <li>HTTP requests executed within the service (e.g. by {@link DetailedEmployeeController})
 * are executed using the regular default implementation of {@link HalResourceLoader} (because we
 * are not using {@link MockMvcHalResourceLoaderConfiguration} here)</li>
 * <li>All URLs in the generated links will be fully qualified</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ExternalClientIT extends AbstractCompanyApiIT {

  private final String entryPointUrl;

  private final CompanyApi companyApi;

  ExternalClientIT(@Autowired ServletWebServerApplicationContext server) {

    HalApiClient apiClient = HalApiClient.create();

    this.entryPointUrl = "http://localhost:" + server.getWebServer().getPort();

    this.companyApi = apiClient.getRemoteResource(entryPointUrl, CompanyApi.class);
  }

  @Override
  protected CompanyApi getApiImplementionOrClientProxy() {

    // All of the tests in the superclass will use this single entry point proxy to execute
    // an HTTP request to the CompanyApiController (exactly as an external consumer would),
    // and then follow links to other resources as required, which will trigger additional
    // requests to the other controllers.
    return companyApi;
  }

  // since we don't have access to the repository in this test, the IDs need to be hard-coded
  @Override
  protected Long getIdOfFirstEmployee() {
    return 2L;
  }

  @Override
  protected Long getIdOfFirstManager() {
    return 1L;
  }

  @Test
  void all_resolved_links_should_lead_to_a_valid_hal_resource_with_titled_self_link() {

    // in addition to the detailed tests in the superclass we also do a quick smoke test here
    // that verifies if any resource that can be reached from the entry point (by following
    // resolved links) can actually be retrieved

    HalCrawler crawler = new HalCrawler(HalResourceLoader.create())
        .withEntryPoint(entryPointUrl);

    MockMvcClientIT.crawlAndVerifyAllResponses(crawler);
  }
}
