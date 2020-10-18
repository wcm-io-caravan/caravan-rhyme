/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.rhyme.jaxrs.api;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.UriInfo;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * A service that asynchronously renders a {@link LinkableResource} to a JAX-RS {@link AsyncResponse}, and handles any
 * errors that happen during rendering
 */
@ProviderType
public interface JaxRsAsyncHalResponseRenderer {

  /**
   * @param resourceImpl the resource to be rendered
   * @param uriInfo information about the URI of the incoming request
   * @param asyncResponse the JAX-RS response to be resumed
   * @param metrics collects information about all upstream requests executed during the current request
   */
  void respondWith(LinkableResource resourceImpl, UriInfo uriInfo, AsyncResponse asyncResponse, RequestMetricsCollector metrics);

}
