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

import static org.osgi.service.component.annotations.ReferenceScope.PROTOTYPE_REQUIRED;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.common.collect.ImmutableMap;

import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.jaxrs.JaxRsBundleInfo;
import io.wcm.caravan.hal.microservices.orchestrator.CaravanJaxRsHalOrchestrator;
import io.wcm.caravan.hal.resource.Link;

@Component(service = ExampleServiceRequestContext.class, scope = ServiceScope.PROTOTYPE)
public class ExampleServiceRequestContext {

  @Reference(scope = PROTOTYPE_REQUIRED)
  private CaravanJaxRsHalOrchestrator orchestrator;

  @Reference
  private JaxRsBundleInfo bundleInfo;

  private Single<ExamplesEntryPointResource> upstreamEntryPoint;

  public ExampleServiceRequestContext() {}

  public ExampleServiceRequestContext(@Context CaravanJaxRsHalOrchestrator orchestrator, JaxRsBundleInfo bundleInfo) {

    this.orchestrator = orchestrator;
    this.bundleInfo = bundleInfo;

    init();
  }

  @Activate
  public void init() {
    limitMaxAge((int)TimeUnit.DAYS.toSeconds(365));
  }


  public Link buildLinkTo(LinkableResource targetResource) {

    Map<String, Object> fingerPrintingParams = ImmutableMap.of("bundleVersion", bundleInfo.getBundleVersion());

    return orchestrator.createLinkBuilder()
        .withAdditionalParameters(fingerPrintingParams)
        .buildLinkTo(targetResource);
  }

  public void limitMaxAge(int seconds) {
    orchestrator.limitOutputMaxAge(seconds);
  }

  public void respondWith(UriInfo uriInfo, LinkableResource resource, AsyncResponse response) {

    orchestrator.respondWith(uriInfo, resource, response);
  }

  public Single<ExamplesEntryPointResource> getUpstreamEntryPoint() {
    if (upstreamEntryPoint == null) {
      upstreamEntryPoint = orchestrator.getEntryPoint(ExamplesEntryPointResource.class);
    }
    return upstreamEntryPoint;
  }

  public String getServiceId() {
    return bundleInfo.getApplicationPath();
  }

  public String getBundleVersion() {
    return bundleInfo.getBundleVersion();
  }

}
