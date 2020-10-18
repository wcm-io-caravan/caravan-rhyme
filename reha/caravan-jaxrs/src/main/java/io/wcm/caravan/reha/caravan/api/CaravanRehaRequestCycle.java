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
package io.wcm.caravan.reha.caravan.api;

import java.util.function.Function;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriInfo;

import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.jaxrs.api.JaxRsAsyncHalResponseRenderer;

/**
 * An OSGI service that will handle an incoming request to a server-side {@link HalApiInterface} implementation
 */
public interface CaravanRehaRequestCycle {

  /**
   * Handles the full request cycle:
   * <ol>
   * <li>Create a {@link CaravanReha} instance</li>
   * <li>Pass that instance on to the given function to create a request context instance</li>
   * <li>Pass that context on to the given function to create the resource implementation to be rendered</li>
   * <li>Render the resource using the {@link JaxRsAsyncHalResponseRenderer} service</li>
   * <li>Catch and handle any exceptions that are thrown in the previous steps</li>
   * </ol>
   * @param requestUri information on the URI of the incoming request
   * @param response the suspended response that will be asynchronously resumed
   * @param requestContextConstructor a function that will create a RequestContextType
   * @param resourceImplConstructor a function that creates the resource implementation to be rendered
   * @param <RequestContextType> a project-specific type that allows to pass on OSGI references and request-specific
   *          information to all resource implementations being created within the request cycle
   */
  <RequestContextType> void processRequest(UriInfo requestUri, AsyncResponse response,
      Function<CaravanReha, RequestContextType> requestContextConstructor,
      Function<RequestContextType, ? extends LinkableResource> resourceImplConstructor);
}
