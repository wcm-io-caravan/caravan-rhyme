/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
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
package io.wcm.caravan.rhyme.api.client;

import java.util.ArrayList;
import java.util.List;

import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.RhymeBuilderUtils;
import io.wcm.caravan.rhyme.impl.client.HalApiClientImpl;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupportAdapter;

public class HalApiClientBuilder {

  private HalResourceLoader resourceLoader;

  private RequestMetricsCollector metrics;

  private final List<HalApiTypeSupport> typeSupports = new ArrayList<>();

  public HalApiClientBuilder withResourceLoader(HalResourceLoader resourceLoader) {

    this.resourceLoader = resourceLoader;
    return this;
  }

  public HalApiClientBuilder withMetrics(RequestMetricsCollector metricsSharedWithClient) {

    this.metrics = metricsSharedWithClient;
    return this;
  }

  public HalApiClientBuilder withReturnTypeSupport(HalApiReturnTypeSupport additionalTypeSupport) {

    if (additionalTypeSupport != null) {
      typeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    }
    return this;
  }

  public HalApiClientBuilder withAnnotationTypeSupport(HalApiAnnotationSupport additionalTypeSupport) {

    if (additionalTypeSupport != null) {
      typeSupports.add(new HalApiTypeSupportAdapter(additionalTypeSupport));
    }
    return this;
  }

  public HalApiClient build() {

    if (resourceLoader == null) {
      resourceLoader = HalResourceLoader.withDefaultHttpClient();
    }

    if (metrics == null) {
      metrics = RequestMetricsCollector.create();
    }

    HalApiTypeSupport effectiveTypeSupport = RhymeBuilderUtils.getEffectiveTypeSupport(typeSupports);

    return new HalApiClientImpl(resourceLoader, metrics, effectiveTypeSupport);
  }
}
