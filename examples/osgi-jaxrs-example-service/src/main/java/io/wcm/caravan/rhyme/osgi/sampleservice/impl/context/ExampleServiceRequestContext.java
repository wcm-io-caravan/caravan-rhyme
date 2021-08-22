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

import static io.wcm.caravan.rhyme.osgi.sampleservice.impl.jaxrs.ExampleServiceJaxRsApplication.BASE_PATH;

import java.time.Duration;
import java.util.Map;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.caravan.api.CaravanRhyme;
import io.wcm.caravan.rhyme.jaxrs.api.JaxRsBundleInfo;
import io.wcm.caravan.rhyme.jaxrs.api.JaxRsLinkBuilder;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.jaxrs.ExampleServiceJaxRsComponent;

public class ExampleServiceRequestContext {

  private static final String BUNDLE_VERSION_QUERY_PARAM = "bundleVersion";

  public static final String LOCALHOST_CARAVAN_SERVICE_ID = "localhost";

  private final CaravanRhyme rhyme;
  private final JaxRsBundleInfo bundleInfo;

  private final ExamplesEntryPointResource upstreamEntryPoint;

  private final JaxRsLinkBuilder<ExampleServiceJaxRsComponent> linkBuilder;

  public ExampleServiceRequestContext(CaravanRhyme rhyme, JaxRsBundleInfo bundleInfo) {

    this.rhyme = rhyme;
    this.bundleInfo = bundleInfo;

    this.upstreamEntryPoint = rhyme.getRemoteResource(LOCALHOST_CARAVAN_SERVICE_ID, BASE_PATH + "/", ExamplesEntryPointResource.class);

    this.linkBuilder = createLinkBuilder(bundleInfo);

    setResponseMaxAge(rhyme);
  }

  private static JaxRsLinkBuilder<ExampleServiceJaxRsComponent> createLinkBuilder(JaxRsBundleInfo bundleInfo) {

    Map<String, Object> fingerPrintingParams = ImmutableMap.of(BUNDLE_VERSION_QUERY_PARAM, bundleInfo.getBundleVersion());

    return JaxRsLinkBuilder.create(bundleInfo.getApplicationPath(), ExampleServiceJaxRsComponent.class)
        .withAdditionalQueryParameters(fingerPrintingParams);
  }

  private static void setResponseMaxAge(CaravanRhyme rhyme) {

    MultivaluedMap<String, String> queryParameters = rhyme.getRequestUri().getQueryParameters();
    boolean fingerPrintingParamPresent = queryParameters.containsKey(BUNDLE_VERSION_QUERY_PARAM);

    Duration maxAge = fingerPrintingParamPresent ? Duration.ofDays(365) : Duration.ofMinutes(1);
    rhyme.setResponseMaxAge(maxAge);
  }

  public interface ControllerCall {

    void call(ExampleServiceJaxRsComponent resource, UriInfo uriInfo, AsyncResponse response);
  }

  public Link buildLinkTo(ControllerCall callToResource) {

    return linkBuilder.buildLinkTo(resource -> {
      callToResource.call(resource, null, null);
    });
  }

  public ExamplesEntryPointResource getUpstreamEntryPoint() {

    return upstreamEntryPoint;
  }

  public boolean hasFingerPrintedUrl() {

    return rhyme.getRequestUri().getQueryParameters().containsKey(BUNDLE_VERSION_QUERY_PARAM);
  }

  public String getBundleVersion() {

    return bundleInfo.getBundleVersion();
  }
}
