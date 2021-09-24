package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test verifies the API functionality when being used directly from within the same application.
 * It executes all tests defined in {@link AbstractCompanyApiIntegrationTest} directly against the server-side
 * resource implementations. It does not verify the link generation or JSON (de)serialization as the
 * {@link ClientSideIntegrationTest} does.
 */
public class ServerSideIntegrationTest extends AbstractCompanyApiIntegrationTest {

  @Autowired // This will inject the {@link CompanyApiController} as it directly implements that interface
  private CompanyApi api;

  @Override
  protected CompanyApi getApi() {
    return api;
  }
}
