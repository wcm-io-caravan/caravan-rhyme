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

import java.time.Duration;

import javax.ws.rs.core.UriInfo;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;

/**
 * An alternative to the generic {@link Rhyme} interface that should be used for OSGi / JAX-RS projects.
 * Use the {@link CaravanRhymeRequestCycle} OSGi service to create one instance for each incoming request.
 */
@ProviderType
public interface CaravanRhyme {

  /**
   * @return information on the URI of the incoming request
   */
  UriInfo getRequestUri();

  /**
   * Create a dynamic client proxy to load HAL+JSON resources from an upstream service.
   * @param <T> an interface annotated with {@link HalApiInterface}
   * @param serviceId the ribbon ID for the upstream service
   * @param uri the absolute path of the entry point
   * @param halApiInterface an interface annotated with {@link HalApiInterface}
   * @return a dynamic proxy instance of the provided {@link HalApiInterface} that you can use to navigate through the
   *         resources of the service
   */
  <T> T getRemoteResource(String serviceId, String uri, Class<T> halApiInterface);

  /**
   * Limit the maximum time for which the response should be cached by clients and downstream services. Note that
   * calling this method only sets the upper limit: if another upstream resource fetched during the current request
   * indicate a lower max-age value in their header, that lower value will be used instead.
   * @param duration the max cache time
   */
  void setResponseMaxAge(Duration duration);

}
