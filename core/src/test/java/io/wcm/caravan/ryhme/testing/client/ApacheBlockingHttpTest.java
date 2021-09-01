package io.wcm.caravan.ryhme.testing.client;

import io.wcm.caravan.rhyme.api.spi.HttpClientImplementation;


public class ApacheBlockingHttpTest extends AbstractHttpClientImplementationTest {

  @Override
  protected HttpClientImplementation createImplementationUnderTest() {
    return new ApacheBlockingHttpImplementation();
  }
}
