
/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.context;

import java.util.Collection;

import javax.ws.rs.Path;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.caching.CachingExamplesResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.caching.EvenAndOddItemsResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.ClientCollectionResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.ClientItemResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.CollectionExamplesResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.DelayableCollectionResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.DelayableItemResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.errors.ErrorsExamplesResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.errors.HalApiClientErrorResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.errors.ServerSideErrorResourceImpl;
import io.wcm.caravan.hal.microservices.jaxrs.JaxRsHalServerSupport;
import io.wcm.caravan.jaxrs.publisher.JaxRsComponent;

@Component(service = JaxRsComponent.class, immediate = true)
@Path("")
public class ExampleServiceOsgiComponent implements JaxRsComponent {

  @Reference
  private JaxRsHalServerSupport halSupport;

  @Override
  public Collection<Class<?>> getAdditionalJaxRsClassesToRegister() {

    return ImmutableList.of(ExampleServiceRequestContext.class, ExamplesEntryPointResourceImpl.class,
        // collections
        CollectionExamplesResourceImpl.class, DelayableCollectionResourceImpl.class, DelayableItemResourceImpl.class,
        ClientCollectionResourceImpl.class, ClientItemResourceImpl.class,
        // caching
        CachingExamplesResourceImpl.class, EvenAndOddItemsResourceImpl.class,
        // errors
        ErrorsExamplesResourceImpl.class, HalApiClientErrorResourceImpl.class, ServerSideErrorResourceImpl.class);
  }

  public JaxRsHalServerSupport getHalSupport() {
    return halSupport;
  }
}
