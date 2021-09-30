package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import io.wcm.caravan.rhyme.spring.testing.MockMvcHalResourceLoaderConfiguration;

/**
 * This test verifies the API functionality when being used from within the same application.
 * It executes all tests defined in {@link AbstractCompanyApiIT} directly against the server-side
 * resource implementations. It does not verify the link generation or JSON (de)serialization as the
 * {@link ExternalClientIT} or {@link MockMvcClientIT} do.
 */
@SpringBootTest
@Import(MockMvcHalResourceLoaderConfiguration.class)
public class ServerSideIT extends AbstractCompanyApiIT {

  @Autowired // This will inject the {@link CompanyApiController} as it implements that interface
  private CompanyApi api;

  @Override
  protected CompanyApi getApiImplementionOrClientProxy() {
    return api;
  }
}