package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import org.springframework.beans.factory.annotation.Autowired;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
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
 * generated in the HAL representation. Whether these links are all correct is tested
 * indirectly by *following* them (exactly as a client would do) and then verify the
 * resource that is being retrieved. This allows the URL structure to be changed at any time
 * without having to adjust the tests. For an API that strictly sticks to the HATEOAS principles,
 * the paths in the URL are only an implementation detail that can be changed (as long as the
 * link relation and template variables in the URI remain the same).
 * </p>
 * <p>
 * The tests also don't make any assumptions on whether specific resources are embedded or not.
 * This is also something that can be changed on the server side without breaking API compatibility
 * </p>
 */
public class ClientSideIntegrationTest extends AbstractCompanyApiIntegrationTest {

  @Autowired
  private MockMvcHalResourceLoader mockMvcResourceLoader;

  @Override
  protected CompanyApi getApi() {

    // Create a HalApiClient that is using spring's MockMvc to simulate actual HTTP requests
    // coming into the currently running WebApplicationContext.
    HalApiClient apiClient = HalApiClient.create(mockMvcResourceLoader);

    // All of the tests in the superclass will now start with fetching the single entry point
    // with an HTTP request to the CompanyApiController (exactly as an external consumer would),
    // and then follow links to other resources as required, which will trigger additional
    // requests to the other controllers.
    return apiClient.getRemoteResource("/", CompanyApi.class);
  }
}
