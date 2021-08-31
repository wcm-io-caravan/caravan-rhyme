package io.wcm.caravan.ryhme.testing.client;

import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;


public class ApacheHttpClientResourceLoaderTest extends HalResourceLoaderTest {

  @Override
  protected HalResourceLoader createLoader() {
    return new ApacheHttpClientResourceLoader();
  }

}
