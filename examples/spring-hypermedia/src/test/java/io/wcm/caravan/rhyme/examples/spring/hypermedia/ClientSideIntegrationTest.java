package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.spring.testing.MockMvcHalCrawler;
import io.wcm.caravan.rhyme.spring.testing.MockMvcHalResourceLoader;

/**
 * This test verifies the API functionality as its seen by an external consumer.
 * It executes all tests defined in {@link AbstractCompanyApiIntegrationTest} using
 * a dynamic client proxy of the {@link CompanyApi}. Without adding further code,
 * it can verify a few aspects that the {@link ServerSideIntegrationTest} doesn't cover.
 * <ul>
 * <li>The path mappings and parameter parsing of the controller methods</li>
 * <li>JSON serialization and deserialization of resource state</li>
 * <li>Generation of the HAL link (templates) on the server side</li>
 * <li>Parsing of the HAL representation on the client side</li>
 * <li>Ensuring that all links are actually pointing to the right controller</li>
 * </ul>
 * <p>
 * Note that these tests don't really look at the URIs or relations of the links being
 * present in the HAL representation. Whether these links are all correct is tested
 * indirectly by *following* them (exactly as a client would do) and then verify the
 * resource that is being retrieved. This allows the URL structure to be changed at any time
 * without having to adjust the tests. For an API that strictly sticks to the HATEOAS principles,
 * the paths in the URL are only an implementation detail that can be changed (as long as the
 * link relation and template variables in the API remain the same).
 * </p>
 * <p>
 * The tests also don't make any assumptions on whether specific resources are embedded or not.
 * This is also something that can be changed on the server side without breaking API compatibility.
 * </p>
 */
public class ClientSideIntegrationTest extends AbstractCompanyApiIntegrationTest {

  @Autowired
  private MockMvcHalResourceLoader mockMvcResourceLoader;

  @Autowired
  private MockMvcHalCrawler crawler;

  @Override
  protected CompanyApi getApiImplementionOrClientProxy() {

    // Create a HalApiClient that is using spring's MockMvc to simulate actual HTTP requests
    // going into the currently running WebApplicationContext.
    HalApiClient apiClient = HalApiClient.create(mockMvcResourceLoader);

    // Return a dynamic client proxy that can fetch the API's entry point resource from the root path
    return apiClient.getRemoteResource("/", CompanyApi.class);

    // All of the tests in the superclass will now start with fetching that single entry point
    // with an HTTP request to the CompanyApiController (exactly as an external consumer would),
    // and then follow links to other resources as required, which will trigger additional
    // requests to the other controllers.
  }

  @Test
  void all_resolved_links_should_lead_to_a_valid_hal_resource() {

    // in addition to the detailed tests in the superclass we also do a quick smoke test here
    // that verifies if any resource that can be reached from the entry point by following
    // resolved links can actually be retrieved
    List<HalResource> allLinkedResources = crawler.getAllResources("/");

    Assertions.assertThat(allLinkedResources)
        .hasSize(18); // this number is expected to change if more test data or resources are added
  }
}
