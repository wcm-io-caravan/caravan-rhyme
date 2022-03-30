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
package io.wcm.caravan.rhyme.aem.impl;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.wcm.caravan.commons.httpclient.HttpClientFactory;
import io.wcm.caravan.rhyme.aem.api.SlingRhymeCustomization;
import io.wcm.caravan.rhyme.aem.impl.client.ApacheHttpClientFactorySupport;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

@Component(service = SlingRhymeCustomizationManager.class, immediate = true)
public class SlingRhymeCustomizationManager {

  private HalResourceLoader resourceLoader;

  @Reference(cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.STATIC,
      policyOption = ReferencePolicyOption.GREEDY)
  private List<SlingRhymeCustomization> customizations;

  @Reference
  private HttpClientFactory clientFactory;

  @Activate
  void activate() {

    resourceLoader = HalResourceLoaderBuilder.create()
        .withCustomHttpClient(new ApacheHttpClientFactorySupport(clientFactory))
        .withMemoryCache()
        .build();
  }

  public HalResourceLoader getResourceLoader() {
    return resourceLoader;
  }

  public void configureRhymeBuilder(RhymeBuilder rhymeBuilder, SlingHttpServletRequest request) {

    for (SlingRhymeCustomization customization : customizations) {
      customization.configureRhymeBuilder(rhymeBuilder, request);
    }
  }
}
