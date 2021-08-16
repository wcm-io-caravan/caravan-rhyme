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
package io.wcm.caravan.rhyme.jaxrs.impl;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.jaxrs.api.JaxRsAsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.jaxrs.impl.docs.RhymeDocsOsgiBundleSupport;

/**
 * OSGI DS component that implements the {@link JaxRsAsyncHalResponseRenderer} interface using the
 * {@link AsyncHalResponseRenderer} and a {@link JaxRsExceptionStrategy}
 */
@Component(service = { JaxRsAsyncHalResponseRenderer.class })
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
public class JaxRsAsyncHalResponseHandlerImpl implements JaxRsAsyncHalResponseRenderer {

  private static final Logger log = LoggerFactory.getLogger(JaxRsAsyncHalResponseHandlerImpl.class);

  private final JaxRsExceptionStrategy exceptionStrategy = new JaxRsExceptionStrategy();

  @Reference
  private RhymeDocsOsgiBundleSupport rhymeDocsLoader;

  @Override
  public void respondWith(LinkableResource resourceImpl, UriInfo uriInfo, AsyncResponse suspended, RequestMetricsCollector metrics) {

    try {
      // create a response renderer with a strategy that is able to extract the status code
      // from any JAX-RS WebApplicationException that might be thrown in the resource implementations
      AsyncHalResponseRenderer renderer = AsyncHalResponseRenderer.create(metrics, exceptionStrategy, null, null, rhymeDocsLoader);

      // asynchronously render the given resource (or create a vnd.error response if any exceptions are thrown)
      String requestUri = getRequestUri(uriInfo);
      Single<HalResponse> rxResponse = renderer.renderResponse(requestUri, resourceImpl);

      rxResponse.subscribe(
          // return the HAL or VND+Error response when it is available
          halResponse -> resumeWithResponse(suspended, halResponse),
          // or fall back to the regular JAX-RS error handling if an exception was not caught
          fatalException -> resumeAfterFatalError(uriInfo, suspended, fatalException));
    }
    // CHECKSTYLE:OFF - we really want to catch any exceptions here
    catch (RuntimeException ex) {
      // CHECKSTYLE:ON
      // when anything fatal happens we still need the response to be resumed with regular JAX-RS error handling
      resumeAfterFatalError(uriInfo, suspended, ex);
    }
  }

  @Override
  public void respondWithError(Throwable ex, UriInfo uriInfo, AsyncResponse suspended, RequestMetricsCollector metrics) {

    try {
      VndErrorResponseRenderer renderer = VndErrorResponseRenderer.create(exceptionStrategy);

      HalResponse response = renderer.renderError(getRequestUri(uriInfo), null, ex, metrics);

      resumeWithResponse(suspended, response);
    }
    // CHECKSTYLE:OFF - we really want to catch any exceptions here
    catch (RuntimeException rex) {
      // CHECKSTYLE:ON
      // when anything fatal happens we still need the response to be resumed with regular JAX-RS error handling
      resumeAfterFatalError(uriInfo, suspended, rex);
    }
  }

  private void resumeWithResponse(AsyncResponse suspended, HalResponse halResponse) {

    // build a JAX-RS response from the HAL response created by the AsyncHalResponseRenderer
    ResponseBuilder jaxRsResponse = Response
        .status(halResponse.getStatus())
        .type(halResponse.getContentType())
        .entity(halResponse.getBody());

    // add a CacheControl header only if a max-age value should be set
    Integer maxAge = halResponse.getMaxAge();
    if (maxAge != null) {
      CacheControl cacheControl = new CacheControl();
      cacheControl.setMaxAge(maxAge);
      jaxRsResponse.cacheControl(cacheControl);
    }

    // send the response to the client (HalResourceMessageBodyWriter will be responsible to serialise the body)
    suspended.resume(jaxRsResponse.build());
  }

  @SuppressWarnings("PMD.GuardLogStatement")
  private void resumeAfterFatalError(UriInfo uriInfo, AsyncResponse suspended, Throwable fatalError) {

    String uri;
    try {
      uri = getRequestUri(uriInfo);
    }
    // CHECKSTYLE:OFF - we really want to catch any exceptions here
    catch (RuntimeException ex) {
      // CHECKSTYLE:ON
      log.error("Failed to get request URI from " + uriInfo, ex);
      uri = "(unknown url)";
    }

    log.error("An exception occured when handling request for " + uri, fatalError);

    suspended.resume(fatalError);
  }

  private static String getRequestUri(UriInfo uriInfo) {
    return uriInfo != null ? uriInfo.getRequestUri().toString() : null;
  }

  /**
   * This strategy allows server-side resource implementations to throw any subclass of {@link WebApplicationException},
   * and ensure that the correct status code is actually added to the {@link HalResponse} instance by the
   * {@link AsyncHalResponseRenderer}
   */
  private static class JaxRsExceptionStrategy implements ExceptionStatusAndLoggingStrategy {

    @Override
    public Integer extractStatusCode(Throwable error) {

      if (error instanceof WebApplicationException) {
        return ((WebApplicationException)error).getResponse().getStatus();
      }

      return null;
    }
  }

}
