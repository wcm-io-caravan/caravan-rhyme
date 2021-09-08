/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.aem.impl.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.commons.httpclient.HttpClientFactory;
import io.wcm.caravan.commons.httpclient.impl.HttpClientFactoryImpl;
import io.wcm.caravan.rhyme.aem.testing.context.AppAemContext;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.testing.client.AbstractHalResourceLoaderTest;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)


public class ApacheHttpClientFactorySupportTest extends AbstractHalResourceLoaderTest {

  private AemContext context = AppAemContext.newAemContext();

  private HttpClientFactory clientFactory;

  @BeforeEach
  void setUp() {
    clientFactory = context.registerInjectActivateService(new HttpClientFactoryImpl());
  }

  @Override
  protected HalResourceLoader createLoaderUnderTest() {

    return HalResourceLoaderBuilder.create()
        .withCustomHttpClient(new ApacheHttpClientFactorySupport(clientFactory))
        .build();
  }

}
