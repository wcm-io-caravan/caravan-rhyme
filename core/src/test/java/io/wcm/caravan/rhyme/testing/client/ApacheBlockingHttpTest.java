package io.wcm.caravan.rhyme.testing.client;

import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;


public class ApacheBlockingHttpTest extends AbstractHalResourceLoaderTest {

  @Override
  protected HalResourceLoader createLoaderUnderTest() {

    return HalResourceLoader.builder()
        .withCustomHttpClient(new ApacheBlockingHttpSupport())
        .build();
  }
}
