package io.wcm.caravan.ryhme.testing.client;

import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;


public class ApacheBlockingHttpTest extends AbstractHttpClientSupportTest {

  @Override
  protected HttpClientSupport createImplementationUnderTest() {
    return new ApacheBlockingHttpSupport();
  }
}
