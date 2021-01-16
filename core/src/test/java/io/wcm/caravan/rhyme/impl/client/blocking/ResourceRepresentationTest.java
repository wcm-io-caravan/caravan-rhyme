/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.rhyme.impl.client.blocking;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceRepresentation;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.relations.StandardRelations;
import io.wcm.caravan.rhyme.api.spi.JsonResourceLoader;
import io.wcm.caravan.ryhme.testing.resources.TestResource;
import io.wcm.caravan.ryhme.testing.resources.TestResourceTree;

/**
 * Variation of the tests in {@link io.wcm.caravan.rhyme.impl.client.ResourceRepresentationTest}
 * for blocking HAL API interfaces (i.e. that are not using reactive return types for their methods)
 */
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
public class ResourceRepresentationTest {

  private RequestMetricsCollector metrics;
  private JsonResourceLoader jsonLoader;
  private TestResource entryPoint;

  @BeforeEach
  public void setUp() {
    metrics = RequestMetricsCollector.create();

    TestResourceTree testResourceTree = new TestResourceTree();
    jsonLoader = testResourceTree;
    entryPoint = testResourceTree.getEntryPoint();

    entryPoint.setText("test");
    entryPoint.setFlag(true);
    entryPoint.createLinked(ITEM);
    entryPoint.createEmbedded(StandardRelations.COLLECTION).createEmbedded(ITEM);
  }

  private <T> T createClientProxy(Class<T> halApiInterface) {
    HalApiClient client = HalApiClient.create(jsonLoader, metrics);
    T clientProxy = client.getEntryPoint(entryPoint.getUrl(), halApiInterface);
    assertThat(clientProxy).isNotNull();
    return clientProxy;
  }

  @HalApiInterface
  interface ResourceWithRepresentations {

    @ResourceRepresentation
    HalResource asHalResource();
  }

  @Test
  public void representation_should_be_available_as_hal_resource() {

    HalResource hal = createClientProxy(ResourceWithRepresentations.class)
        .asHalResource();

    assertThat(hal.getModel()).isEqualTo(entryPoint.getJson());
  }

}
