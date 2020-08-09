/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.caravan.reha.caravan.impl;

import java.time.Duration;
import java.util.function.Function;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.caravan.api.CaravanHalApiClient;
import io.wcm.caravan.reha.caravan.api.CaravanReha;
import io.wcm.caravan.reha.caravan.api.CaravanRehaBuilder;
import io.wcm.caravan.reha.jaxrs.api.JaxRsAsyncHalResponseHandler;

@Component(service = CaravanRehaBuilder.class)
public class CaravanRehaBuilderImpl implements CaravanRehaBuilder {

  @Reference
  private CaravanHttpClient httpClient;

  @Reference
  private JaxRsAsyncHalResponseHandler responseHandler;

  @Reference
  private CaravanHalApiClient halApiClient;

  @Override
  public CaravanReha buildForRequestTo(UriInfo requestUri, AsyncResponse response) {
    return new CaravanRehaImpl(requestUri, response);
  }

  private final class CaravanRehaImpl implements CaravanReha {

    private final RequestMetricsCollector metrics = RequestMetricsCollector.create();

    private final UriInfo requestUri;
    private final AsyncResponse response;

    public CaravanRehaImpl(UriInfo requestUri, AsyncResponse response) {
      this.requestUri = requestUri;
      this.response = response;
    }

    @Override
    public UriInfo getRequestUri() {
      return requestUri;
    }

    @Override
    public void setResponseMaxAge(Duration duration) {

      metrics.setResponseMaxAge(duration);
    }

    @Override
    public <T> T getEntryPoint(String serviceId, String uri, Class<T> halApiInterface) {

      return halApiClient.getEntryPoint(serviceId, uri, halApiInterface, metrics);
    }

    @Override
    public <RequestContextType> void processRequest(
        Function<CaravanReha, RequestContextType> requestContextConstructor,
        Function<RequestContextType, ? extends LinkableResource> resourceImplConstructor) {

      RequestContextType requestContext = requestContextConstructor.apply(this);

      LinkableResource resource = resourceImplConstructor.apply(requestContext);

      responseHandler.respondWith(resource, requestUri, response, metrics);
    }

  }

}
