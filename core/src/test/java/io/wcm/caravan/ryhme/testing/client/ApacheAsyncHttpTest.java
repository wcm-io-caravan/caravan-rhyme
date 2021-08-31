package io.wcm.caravan.ryhme.testing.client;

import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;


public class ApacheAsyncHttpTest extends HalResourceLoaderTest {

  @Override
  protected HalResourceLoader createLoader() {
    return new HttpHalResourceLoader(new ApacheAsyncHttpImplementation());
  }

}
