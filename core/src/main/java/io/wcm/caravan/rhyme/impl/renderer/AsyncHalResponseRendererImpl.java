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
package io.wcm.caravan.rhyme.impl.renderer;

import org.apache.commons.lang3.StringUtils;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;
import io.wcm.caravan.rhyme.impl.documentation.RhymeDocsCurieGenerator;
import io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations;
import io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils;

/**
 * A full implementation of {@link AsyncHalResourceRenderer} that uses {@link VndErrorResponseRendererImpl}
 * to handle any errors that occurred when rendering the {@link HalResource}
 */
public class AsyncHalResponseRendererImpl implements AsyncHalResponseRenderer {

  private final AsyncHalResourceRenderer renderer;

  private final RequestMetricsCollector metrics;

  private final VndErrorResponseRenderer errorRenderer;

  private final HalApiAnnotationSupport annotationSupport;

  private final RhymeDocsCurieGenerator curieGenerator;

  /**
   * @param renderer used to asynchronously render a {@link HalResource}
   * @param metrics an instance of {@link RequestMetricsCollector} to collect performance and caching information for
   *          the current incoming request
   * @param exceptionStrategy allows controlling the status code and logging of exceptions being thrown during rendering
   * @param annotationSupport the strategy to detect HAL API annotations
   * @param rhymeDocsSupport to determine the base URL where documentation is mounted. Can be null, but then no curies
   *          will be generated
   */
  public AsyncHalResponseRendererImpl(AsyncHalResourceRenderer renderer, RequestMetricsCollector metrics,
      ExceptionStatusAndLoggingStrategy exceptionStrategy, HalApiAnnotationSupport annotationSupport, RhymeDocsSupport rhymeDocsSupport) {
    this.renderer = renderer;
    this.metrics = metrics;
    this.errorRenderer = VndErrorResponseRenderer.create(exceptionStrategy);
    this.annotationSupport = annotationSupport;

    if (rhymeDocsSupport != null) {
      this.curieGenerator = new RhymeDocsCurieGenerator(rhymeDocsSupport);
    }
    else {
      this.curieGenerator = null;
    }
  }

  @Override
  public Single<HalResponse> renderResponse(String requestUri, LinkableResource resourceImpl) {

    try {
      return renderer.renderResource(resourceImpl)
          .map(halResource -> createResponse(requestUri, resourceImpl, halResource))
          // for async HalApiInterfaces, errors are usually emitted from the Single...
          .onErrorReturn(ex -> errorRenderer.renderError(requestUri, resourceImpl, ex, metrics));
    }
    // CHECKSTYLE:OFF  ... but especially for HalApiInterfaces with blocking return values, they can also be immediately thrown in the invocation
    catch (RuntimeException ex) {
      // CHECKSTYLE:ON
      return Single.just(errorRenderer.renderError(requestUri, resourceImpl, ex, metrics));
    }
  }

  HalResponse createResponse(String requestUri, LinkableResource resourceImpl, HalResource halResource) {

    Class<?> halApiInterface = HalApiReflectionUtils.findHalApiInterface(resourceImpl, annotationSupport);

    if (curieGenerator != null) {
      curieGenerator.addCuriesTo(halResource, halApiInterface);
    }

    addMetadata(metrics, halResource, resourceImpl);

    String contentType = getContentTypeFromAnnotation(halApiInterface);

    return new HalResponse()
        .withUri(requestUri)
        .withStatus(200)
        .withContentType(contentType)
        .withBody(halResource)
        .withMaxAge(metrics.getResponseMaxAge());
  }

  private String getContentTypeFromAnnotation(Class<?> halApiInterface) {

    String contentType = annotationSupport.getContentType(halApiInterface);

    if (StringUtils.isNotBlank(contentType)) {
      return contentType;
    }
    return HalResource.CONTENT_TYPE;
  }

  static void addMetadata(RequestMetricsCollector metrics, HalResource hal, LinkableResource resourceImpl) {

    HalResource metadata = metrics.createMetadataResource(resourceImpl);
    if (metadata != null) {
      hal.addEmbedded(ResponseMetadataRelations.RHYME_METADATA_RELATION, metadata);
    }
  }

  public HalApiAnnotationSupport getAnnotationSupport() {
    return annotationSupport;
  }
}
