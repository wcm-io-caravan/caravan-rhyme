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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.observers.TestObserver;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceOsgiComponent;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.microservices.api.common.RequestMetricsCollector;
import io.wcm.caravan.hal.microservices.caravan.CaravanHalApiClient;
import io.wcm.caravan.hal.microservices.jaxrs.JaxRsHalServerSupport;

@RunWith(MockitoJUnitRunner.class)
public class ClientCollectionResourceImplTest {

  @Rule
  public OsgiContext osgi = new OsgiContext();

  @Mock
  private JaxRsHalServerSupport support;

  ExampleServiceRequestContext requestContext;

  @Before
  public void setUp() {

    osgi.registerService(JaxRsHalServerSupport.class, support);

    ExampleServiceOsgiComponent osgiComponent = new ExampleServiceOsgiComponent();
    //osgi.registerInjectActivateService(osgiComponent);

    Mockito.when(support.getHalApiClient()).thenReturn(new MockingHalApiClient());

    requestContext = new ExampleServiceRequestContext(osgiComponent);
  }

  @Test
  public void testGetItems() throws Exception {
    ClientCollectionResourceImpl resource = new ClientCollectionResourceImpl(requestContext, null);

    TestObserver<ItemResource> observer = TestObserver.create();
    //resource.getItems().subscribe(observer);
  }

  class MockingHalApiClient implements CaravanHalApiClient {

    @Override
    public <T> T getEntryPoint(String serviceId, String uri, Class<T> halApiInterface, RequestMetricsCollector metrics) {
      // TODO: Auto-generated method stub
      return null;
    }

  }

}
