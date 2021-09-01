package io.wcm.caravan.ryhme.testing.client;

import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;


public class ApacheAsyncHttpTest extends AbstractHttpClientSupportTest {

  @Override
  protected HttpClientSupport createImplementationUnderTest() {
    return new ApacheAsyncHttpSupport();
  }

}
