package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.spring.testing.HalCrawler;
import io.wcm.caravan.rhyme.spring.testing.MockMvcHalResourceLoaderConfiguration;

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
public class ExternalClientIT extends AbstractCompanyApiIT {

  private static final String ENTRY_POINT_URL = "http://localhost:8081";

  private static Thread thread;

  @BeforeAll
  public static void startApplication() throws Exception {

    // TODO: create a JUnit extension that properly takes care of application startup
    thread = new Thread(() -> SpringRhymeHypermediaApplication.main(new String[0]));
    thread.start();

    await()
        .atMost(Duration.ofSeconds(30))
        .alias("Spring Boot application responds to HTTP request")
        .untilAsserted(() -> assertThatEntryPointCanBeLoaded());
  }

  private static void assertThatEntryPointCanBeLoaded() {

    try {
      HttpURLConnection connection = (HttpURLConnection)URI.create(ENTRY_POINT_URL).toURL().openConnection();
      connection.connect();

      assertThat(connection.getResponseCode())
          .as("Response code of " + ENTRY_POINT_URL)
          .isEqualTo(200);
    }
    catch (IOException ex) {
      Assertions.fail("An exception was caught", ex);
    }
  }

  // since we don't have access to the repository, the IDs need to be hard-coded for this test
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

    // Create a HalApiClient that is using a UrlHttpConnection to execute actual HTTP requests
    // against the spring application that was started
    HalApiClient apiClient = HalApiClient.create();

    // Return a dynamic client proxy that can fetch the API's entry point resource from the root path
    return apiClient.getRemoteResource(ENTRY_POINT_URL, CompanyApi.class);

    // All of the tests in the superclass will now start with fetching that single entry point
    // with an HTTP request to the CompanyApiController (exactly as an external consumer would),
    // and then follow links to other resources as required, which will trigger additional
    // requests to the other controllers.
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
