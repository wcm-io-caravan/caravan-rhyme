package io.wcm.caravan.rhyme.spring.impl;

import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.testing.client.AbstractHalResourceLoaderTest;

public class WebClientHalResourceLoaderTest extends AbstractHalResourceLoaderTest {

	@Override
	protected HalResourceLoader createLoaderUnderTest() {
		// make sure to disable caching for these unit-tests
		return new WebClientHalResourceLoader(false);
	}
}
