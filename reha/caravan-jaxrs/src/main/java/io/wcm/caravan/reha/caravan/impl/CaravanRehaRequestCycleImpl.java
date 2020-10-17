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

import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.caravan.api.CaravanHalApiClient;
import io.wcm.caravan.reha.caravan.api.CaravanReha;
import io.wcm.caravan.reha.caravan.api.CaravanRehaRequestCycle;
import io.wcm.caravan.reha.jaxrs.api.JaxRsAsyncHalResponseHandler;

/**
 * Implementations of the {@link CaravanRehaRequestCycle} and {@link CaravanReha} interfaces
 */
@Component(service = CaravanRehaRequestCycle.class)
public class CaravanRehaRequestCycleImpl implements CaravanRehaRequestCycle {

  @Reference
  private JaxRsAsyncHalResponseHandler responseHandler;

  @Reference
  private CaravanHalApiClient halApiClient;

  @Override
  public <RequestContextType> void processRequest(UriInfo requestUri, AsyncResponse response,
      Function<CaravanReha, RequestContextType> requestContextConstructor, Function<RequestContextType, ? extends LinkableResource> resourceImplConstructor) {

    CaravanRehaImpl rhyme = createRhymeInstance(requestUri);

    RequestContextType requestContext = requestContextConstructor.apply(rhyme);

    LinkableResource resource = resourceImplConstructor.apply(requestContext);

    responseHandler.respondWith(resource, requestUri, response, rhyme.metrics);
  }

  CaravanRehaImpl createRhymeInstance(UriInfo requestUri) {

    return new CaravanRehaImpl(halApiClient, requestUri);
  }

  static final class CaravanRehaImpl implements CaravanReha {

    private final RequestMetricsCollector metrics = RequestMetricsCollector.create();

    private final CaravanHalApiClient halApiClient;

    private final UriInfo requestUri;

    CaravanRehaImpl(CaravanHalApiClient halApiClient, UriInfo requestUri) {
      this.halApiClient = halApiClient;
      this.requestUri = requestUri;
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
  }
}
