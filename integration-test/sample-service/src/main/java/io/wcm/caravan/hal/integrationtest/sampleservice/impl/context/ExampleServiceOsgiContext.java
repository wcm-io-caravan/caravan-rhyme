
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

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.google.common.collect.ImmutableList;

import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.CollectionExamplesResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.ItemCollectionResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.ItemResourceImpl;
import io.wcm.caravan.jaxrs.publisher.ApplicationPath;
import io.wcm.caravan.jaxrs.publisher.JaxRsComponent;

@Component(service = JaxRsComponent.class, immediate = true)
@Path("")
public class ExampleServiceOsgiContext implements JaxRsComponent {

  private String contextPath;

  @Activate
  void activate(BundleContext bundleCtx) {
    contextPath = ApplicationPath.get(bundleCtx);
  }

  @Override
  public Collection<Class<?>> getChildComponentClasses() {
    System.out.println(getClass().getSimpleName() + " was asked for child component classes");

    return ImmutableList.of(ExamplesEntryPointResourceImpl.class, CollectionExamplesResourceImpl.class, ItemCollectionResourceImpl.class,
        ItemResourceImpl.class);
  }

  public ExampleServiceRequestContext createRequestContext() {
    return new ExampleServiceRequestContext(contextPath);
  }

}
