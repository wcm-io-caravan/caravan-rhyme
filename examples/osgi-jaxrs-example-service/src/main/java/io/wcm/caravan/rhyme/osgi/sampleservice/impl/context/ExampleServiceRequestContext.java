/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
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
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.context;

import static io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceApplication.BASE_PATH;

import java.time.Duration;
import java.util.Map;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriInfo;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.caravan.api.CaravanRhyme;
import io.wcm.caravan.rhyme.jaxrs.api.JaxRsBundleInfo;
import io.wcm.caravan.rhyme.jaxrs.api.JaxRsLinkBuilder;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;

public class ExampleServiceRequestContext {

  public static final String LOCALHOST_CARAVAN_SERVICE_ID = "localhost";

  private CaravanRhyme rhyme;

  private ExamplesEntryPointResource upstreamEntryPoint;

  private JaxRsLinkBuilder<ExampleServiceJaxRsComponent> linkBuilder;

  public ExampleServiceRequestContext(CaravanRhyme rhyme, JaxRsBundleInfo bundleInfo) {
    this.rhyme = rhyme;

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
    rhyme.setResponseMaxAge(duration);
  }

  public ExamplesEntryPointResource getUpstreamEntryPoint() {
    if (upstreamEntryPoint == null) {
      upstreamEntryPoint = rhyme.getRemoteResource(LOCALHOST_CARAVAN_SERVICE_ID, BASE_PATH, ExamplesEntryPointResource.class);
    }
    return upstreamEntryPoint;
  }
}
