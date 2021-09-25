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
package io.wcm.caravan.rhyme.spring.api;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.client.HalApiClient;

/**
 * A request-scoped {@link Component} that you need to inject into your controllers if you want to
 * load remote resources from an upstream service, or control the max-age cache-control directive of the response
 * that you are rendering. This component wraps the same {@link Rhyme} instance that is also used to render the
 * server-side resource implementations that you return in your {@link RestController} methods.
 */
public interface SpringRhyme {

  /**
   * Create a dynamic client proxy to load HAL+JSON resources from an upstream service (using the core framework's
   * {@link HalApiClient})
   * @param <T> an interface annotated with {@link HalApiInterface}
   * @param uri the fully qualified URI of the entry point
   * @param halApiInterface an interface annotated with {@link HalApiInterface}
   * @return a dynamic proxy instance of the provided {@link HalApiInterface} that you can use to navigate through the
   *         resources of the service
   * @see HalApiClient
   */
  <T> T getRemoteResource(String uri, Class<T> halApiInterface);

  /**
   * Limit the maximum time for which the response should be cached by clients and downstream services. Note that
   * calling this method only sets the upper limit: if other upstream resource fetched during the current request
   * indicate a lower max-age value in their header, that lower value will be used instead.
   * @param duration the max cache time
   */
  void setResponseMaxAge(Duration duration);

  /**
   * Enables support for timestamp-based URL fingerprinting for the current request.
   * @return a {@link UrlFingerprinting} instance that you need to configure and then use to build the links for all
   *         resources in your project
   * @see UrlFingerprinting
   */
  UrlFingerprinting enableUrlFingerprinting();

  /**
   * Provides access to the underlying {@link Rhyme} instance of the core framework to be used for the current request,
   * just in case you need to call one of the methods for which there doesn't exist a delegate in the
   * {@link SpringRhyme} interface
   * @return the single {@link Rhyme} instance that is used throughout the incoming request
   */
  Rhyme getCoreRhyme();
}
