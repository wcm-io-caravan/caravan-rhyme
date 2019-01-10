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
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Context;

import com.google.common.collect.ImmutableMap;

import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.microservices.jaxrs.JaxRsBundleInfo;
import io.wcm.caravan.hal.microservices.orchestrator.CaravanJaxRsHalOrchestrator;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.jaxrs.publisher.OsgiReference;

@Path("")
public class ExampleServiceRequestContext {

  private final CaravanJaxRsHalOrchestrator orchestrator;

  private final JaxRsBundleInfo bundleInfo;

  private final Single<ExamplesEntryPointResource> upstreamEntryPoint;

  public ExampleServiceRequestContext(@Context CaravanJaxRsHalOrchestrator orchestrator, @OsgiReference JaxRsBundleInfo bundleInfo) {

    this.orchestrator = orchestrator;
    this.bundleInfo = bundleInfo;

    this.upstreamEntryPoint = orchestrator.getEntryPoint(ExamplesEntryPointResource.class);

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

  public void respondWith(LinkableResource resource, AsyncResponse response) {
    orchestrator.respondWith(resource, response);
  }

  public Single<ExamplesEntryPointResource> getUpstreamEntryPoint() {
    return upstreamEntryPoint;
  }

  public String getServiceId() {
    return bundleInfo.getApplicationPath();
  }

  public String getBundleVersion() {
    return bundleInfo.getBundleVersion();
  }
}
