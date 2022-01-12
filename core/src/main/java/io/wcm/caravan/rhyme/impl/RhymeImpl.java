/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
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
package io.wcm.caravan.rhyme.impl;

import java.time.Duration;
import java.util.function.Supplier;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;

final class RhymeImpl implements Rhyme {

  private final String incomingRequestUri;
  private final VndErrorResponseRenderer errorRenderer;
  private final HalApiClient client;
  private final AsyncHalResponseRenderer renderer;
  private final RequestMetricsCollector metrics;

  RhymeImpl(String incomingRequestUri, HalApiClient client, AsyncHalResponseRenderer renderer, VndErrorResponseRenderer errorRenderer,
      RequestMetricsCollector metrics) {

    this.incomingRequestUri = incomingRequestUri;
    this.errorRenderer = errorRenderer;
    this.client = client;
    this.renderer = renderer;
    this.metrics = metrics;
  }

  @Override
  public <T> T getRemoteResource(String uri, Class<T> halApiInterface) {

    return client.getRemoteResource(uri, halApiInterface);
  }

  @Override
  public void setResponseMaxAge(Duration duration) {

    metrics.setResponseMaxAge(duration);
  }

  @Override
  public Single<HalResponse> renderResponse(LinkableResource resourceImpl) {

    return renderer.renderResponse(incomingRequestUri, resourceImpl);
  }

  @Override
  public HalResponse renderVndErrorResponse(Throwable error) {

    return errorRenderer.renderError(incomingRequestUri, null, error, metrics);
  }

  @Override
  public RequestMetricsStopwatch startStopwatch(Class clazz, Supplier<String> taskDescription) {

    return metrics.startStopwatch(clazz, taskDescription);
  }
}