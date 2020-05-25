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

import java.time.Duration;
import java.util.Map;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriInfo;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.microservices.caravan.CaravanReha;
import io.wcm.caravan.hal.microservices.jaxrs.JaxRsBundleInfo;
import io.wcm.caravan.hal.microservices.jaxrs.JaxRsLinkBuilder;
import io.wcm.caravan.hal.resource.Link;

public class ExampleServiceRequestContext {

  private CaravanReha reha;
  private JaxRsBundleInfo bundleInfo;

  private ExamplesEntryPointResource upstreamEntryPoint;

  private JaxRsLinkBuilder<ExampleServiceJaxRsComponent> linkBuilder;

  public ExampleServiceRequestContext(CaravanReha reha, JaxRsBundleInfo bundleInfo) {
    this.reha = reha;
    this.bundleInfo = bundleInfo;

    this.linkBuilder = createLinkBuilder(bundleInfo);

    limitMaxAge(Duration.ofDays(365));
  }

  private static JaxRsLinkBuilder<ExampleServiceJaxRsComponent> createLinkBuilder(JaxRsBundleInfo bundleInfo) {

    Map<String, Object> fingerPrintingParams = ImmutableMap.of("bundleVersion", bundleInfo.getBundleVersion());

    return JaxRsLinkBuilder.create(bundleInfo.getApplicationPath(), ExampleServiceJaxRsComponent.class)
        .withAdditionalQueryParameters(fingerPrintingParams);
  }

  public interface ControllerCall {

    void call(ExampleServiceJaxRsComponent resource, UriInfo uriInfo, AsyncResponse response);
  }

  public Link buildLinkTo(ControllerCall callToResource) {

    return linkBuilder.buildLinkTo(resource -> {
      callToResource.call(resource, null, null);
    });
  }

  public void limitMaxAge(Duration duration) {
    reha.setResponseMaxAge(duration);
  }

  public ExamplesEntryPointResource getUpstreamEntryPoint() {
    if (upstreamEntryPoint == null) {
      String serviceId = getServiceId();
      upstreamEntryPoint = reha.getEntryPoint(serviceId, serviceId, ExamplesEntryPointResource.class);
    }
    return upstreamEntryPoint;
  }

  public String getServiceId() {
    return bundleInfo.getApplicationPath();
  }
}
