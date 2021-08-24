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
package io.wcm.caravan.rhyme.impl;

import java.time.Duration;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;
import io.wcm.caravan.rhyme.impl.client.HalApiClientImpl;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRenderer;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererImpl;
import io.wcm.caravan.rhyme.impl.renderer.AsyncHalResponseRendererImpl;

final class RhymeImpl implements Rhyme {

  private final RequestMetricsCollector metrics = RequestMetricsCollector.create();

  private final String requestUri;
  private final HalResourceLoader resourceLoader;
  private final ExceptionStatusAndLoggingStrategy exceptionStrategy;

  private final HalApiClient client;
  private final AsyncHalResponseRenderer renderer;

  RhymeImpl(String requestUri, HalResourceLoader resourceLoader, ExceptionStatusAndLoggingStrategy exceptionStrategy, HalApiTypeSupport typeSupport,
      RhymeDocsSupport rhymeDocsSupport) {
    this.requestUri = requestUri;
    this.resourceLoader = resourceLoader;
    this.exceptionStrategy = exceptionStrategy;
    this.client = createHalApiClient(typeSupport);
    this.renderer = createResponseRenderer(typeSupport, rhymeDocsSupport);
  }

  private HalApiClient createHalApiClient(HalApiTypeSupport typeSupport) {

    if (resourceLoader == null) {
      return new HalApiClient() {

        @Override
        public <T> T getRemoteResource(String uri, Class<T> halApiInterface) {
          throw new HalApiDeveloperException("#getRemoteResource can only be used if you have provided a " + HalResourceLoader.class.getSimpleName()
              + " when constructing your " + RhymeBuilder.class.getSimpleName());
        }
      };
    }

    return new HalApiClientImpl(resourceLoader, metrics, typeSupport);
  }

  private AsyncHalResponseRenderer createResponseRenderer(HalApiTypeSupport typeSupport, RhymeDocsSupport docsSupport) {

    AsyncHalResourceRenderer resourceRenderer = new AsyncHalResourceRendererImpl(metrics, typeSupport);

    return new AsyncHalResponseRendererImpl(resourceRenderer, metrics, exceptionStrategy, typeSupport, docsSupport);
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

    return renderer.renderResponse(requestUri, resourceImpl);
  }

  @Override
  public HalResponse renderVndErrorResponse(Throwable error) {

    VndErrorResponseRenderer errorRenderer = VndErrorResponseRenderer.create(exceptionStrategy);

    return errorRenderer.renderError(requestUri, null, error, metrics);
  }
}
