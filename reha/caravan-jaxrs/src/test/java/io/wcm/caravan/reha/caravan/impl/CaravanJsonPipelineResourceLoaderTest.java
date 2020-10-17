/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.caravan.reha.caravan.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codahale.metrics.MetricRegistry;

import io.wcm.caravan.pipeline.JsonPipelineFactory;
import io.wcm.caravan.pipeline.impl.JsonPipelineFactoryImpl;
import io.wcm.caravan.reha.api.spi.JsonResourceLoader;

@ExtendWith(MockitoExtension.class)
public class CaravanJsonPipelineResourceLoaderTest extends AbstractCaravanJsonResourceLoaderTest {

  private JsonPipelineFactory pipelineFactory;

  @Mock
  private MetricRegistry metricRegistry;

  private JsonResourceLoader resourceLoader;

  @Override
  protected JsonResourceLoader getResourceLoader() {
    return resourceLoader;
  }

  @BeforeEach
  void setUp() {
    pipelineFactory = new JsonPipelineFactoryImpl(httpClient, metricRegistry);
    resourceLoader = new CaravanJsonPipelineResourceLoader(pipelineFactory, EXTERNAL_SERVICE_ID);
  }
}
