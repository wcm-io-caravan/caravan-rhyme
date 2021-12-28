package io.wcm.caravan.rhyme.testing.client;

import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;


public class ApacheAsyncHttpTest extends AbstractHalResourceLoaderTest {

  @Override
  protected HalResourceLoader createLoaderUnderTest() {

    return HalResourceLoader.builder()
        .withCustomHttpClient(new ApacheAsyncHttpSupport())
        .build();
  }

}
