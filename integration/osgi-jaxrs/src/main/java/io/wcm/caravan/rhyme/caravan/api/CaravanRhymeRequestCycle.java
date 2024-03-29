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
package io.wcm.caravan.rhyme.caravan.api;

import java.util.function.Function;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriInfo;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.jaxrs.api.JaxRsAsyncHalResponseRenderer;

/**
 * An OSGI service that will handle an incoming request to a server-side {@link HalApiInterface} implementation
 */
@ProviderType
public interface CaravanRhymeRequestCycle {

  /**
   * Handles the full request cycle:
   * <ol>
   * <li>Create a {@link CaravanRhyme} instance</li>
   * <li>Pass that instance on to the given function to create a request context instance</li>
   * <li>Pass that context on to the given function to create the resource implementation to be rendered</li>
   * <li>Render the resource using the {@link JaxRsAsyncHalResponseRenderer} service</li>
   * <li>Catch and handle any exceptions that are thrown in the previous steps</li>
   * </ol>
   * @param requestUri information on the URI of the incoming request
   * @param response the suspended response that will be asynchronously resumed
   * @param requestContextConstructor a function that will create a RequestContextType
   * @param resourceImplConstructor a function that creates the resource implementation to be rendered
   * @param <T> a project-specific type that allows to pass on OSGI references and request-specific
   *          information to all resource implementations being created within the request cycle
   */
  <T> void processRequest(UriInfo requestUri, AsyncResponse response,
      Function<CaravanRhyme, T> requestContextConstructor,
      Function<T, ? extends LinkableResource> resourceImplConstructor);
}
