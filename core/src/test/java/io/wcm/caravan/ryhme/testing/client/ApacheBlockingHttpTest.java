package io.wcm.caravan.ryhme.testing.client;

import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;


public class ApacheBlockingHttpTest extends AbstractHttpClientImplementationTest {

  @Override
  protected HalResourceLoader createLoader() {
    return new HttpHalResourceLoader(new ApacheBlockingHttpImplementation());
  }

}
