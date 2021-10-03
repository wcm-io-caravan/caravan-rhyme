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

import java.net.HttpURLConnection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.spring.testing.HalCrawler;
import io.wcm.caravan.rhyme.spring.testing.MockMvcHalResourceLoaderConfiguration;
import io.wcm.caravan.rhyme.spring.testing.SpringRhymeIntegrationTest;
import io.wcm.caravan.rhyme.spring.testing.SpringRhymeIntegrationTestExtension;

/**
 * This test is similar to {@link MockMvcClientIT}, but it's actually starting
 * the Spring boot application with the main method, and is using a {@link HttpURLConnection}
 * to load the resources under test exactly as a real consumer would.
 * Differences in behaviour to {@link MockMvcClientIT} are:
 * <ul>
 * <li>HTTP requests executed within the service (e.g. by {@link DetailedEmployeeController})
 * are executed using the regular default implementation of {@link HalResourceLoader} (because we
 * are not using {@link MockMvcHalResourceLoaderConfiguration} here)</li>
 * <li>All URLs in the generated links will be fully qualified</li>
 * <li>It is verified that the application properly starts up using the main method</li>
 * </ul>
 */
@ExtendWith(SpringRhymeIntegrationTestExtension.class)
@SpringRhymeIntegrationTest(
    entryPointUri = ExternalClientIT.ENTRY_POINT_URL,
    applicationClass = SpringRhymeHypermediaApplication.class)
public class ExternalClientIT extends AbstractCompanyApiIT {

  static final String ENTRY_POINT_URL = "http://localhost:8081";

  private final CompanyApi companyApi;

  /**
   * @param companyApi a client proxy created by the {@link SpringRhymeIntegrationTestExtension}
   */
  ExternalClientIT(CompanyApi companyApi) {
    this.companyApi = companyApi;
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

  @Override
  protected CompanyApi getApiImplementionOrClientProxy() {

    // All of the tests in the superclass will use this single entry point proxy to execute
    // an HTTP request to the CompanyApiController (exactly as an external consumer would),
    // and then follow links to other resources as required, which will trigger additional
    // requests to the other controllers.
    return companyApi;
  }

  @Test
  void all_resolved_links_should_lead_to_a_valid_hal_resource_with_titled_self_link() {

    // in addition to the detailed tests in the superclass we also do a quick smoke test here
    // that verifies if any resource that can be reached from the entry point (by following
    // resolved links) can actually be retrieved

    HalCrawler crawler = new HalCrawler(HalResourceLoader.builder().build())
        .withEntryPoint(ENTRY_POINT_URL);

    MockMvcClientIT.crawlAndVerifyAllResponses(crawler);
  }
}
