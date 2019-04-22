
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

import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.ImmutableMap;

import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.microservices.jaxrs.JaxRsBundleInfo;
import io.wcm.caravan.hal.microservices.orchestrator.UpstreamServiceRegistry;

/**
 * An OSGI service that register all classes that should be managed by JAX-RS (with request-scope).
 */
@Component(service = { UpstreamServiceRegistry.class }, immediate = true)
public class ExampleServiceOsgiComponent implements UpstreamServiceRegistry {

  @Reference
  private JaxRsBundleInfo bundleInfo;

  @Override
  public Single<Map<Class, String>> getUpstreamServiceIds(UriInfo infomingRequestUri) {

    return Single.just(ImmutableMap.of(ExamplesEntryPointResource.class, ExampleServiceApplication.BASE_PATH));
  }

  @Override
  public Single<String> getEntryPointUri(String serviceId, UriInfo incomingRequestUri) {

    return Single.just(serviceId);
  }
}
