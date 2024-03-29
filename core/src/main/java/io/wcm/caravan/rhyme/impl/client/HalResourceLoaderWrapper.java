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
package io.wcm.caravan.rhyme.impl.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.util.RxJavaTransformers;

/**
 * Extends another {@link HalResourceLoader} with common error handling and metrics collection functionality
 */
class HalResourceLoaderWrapper implements HalResourceLoader {

  private static final Logger log = LoggerFactory.getLogger(HalResourceLoaderWrapper.class);

  private final Cache<String, Single<HalResponse>> cache = CacheBuilder.newBuilder().build();

  private final HalResourceLoader delegate;
  private final RequestMetricsCollector metrics;

  HalResourceLoaderWrapper(HalResourceLoader delegate, RequestMetricsCollector metrics) {
    this.delegate = delegate;
    this.metrics = metrics;
  }

  @Override
  @SuppressWarnings("PMD.PreserveStackTrace")
  public Single<HalResponse> getHalResource(String uri) {
    try {
      // repeated calls for same URI should return the same instance
      return cache.get(uri, () -> {

        // don't start the timer right now, as it can still take some time before the request is actually
        // started (by a subscription to the Single that is being returned)
        Stopwatch stopwatch = Stopwatch.createUnstarted();

        // load the resource
        return delegate.getHalResource(uri)
            // capture response performance and metadata
            .doOnSubscribe(d -> startStopwatch(stopwatch))
            .doOnError(ex -> registerErrorMetrics(uri, ex, stopwatch))
            .doOnSuccess(response -> registerResponseMetrics(uri, response, stopwatch))
            // ensure that only HalApiClientException are emitted
            .onErrorResumeNext(ex -> rethrowUnexpectedExceptions(uri, ex))
            // rewrite any links that are not fully qualified
            .map(response -> new LinkRewriting(uri).resolveRelativeLinks(response))
            // just returning the same Single instance isn't enough, we also have to transform the Single into one
            // that will actually replay the result for each subscriber, but only if it succeeded (as the retry operator should still be usable)
            .compose(RxJavaTransformers.cacheSingleIfCompleted());
      });
    }
    catch (UncheckedExecutionException | ExecutionException ex) {
      String msg = delegate.getClass() + "#getHalResource(String) returned null or threw an exception. "
          + " Please make sure that your implementation will always return a Single instance during assembly time.";
      throw new HalApiDeveloperException(msg, ex.getCause());
    }
  }

  private void startStopwatch(Stopwatch stopwatch) {
    // even we are trying to avoid multiple requests being executed for the same URL, there can still
    // be multiple subscriptions to the Single in case that an error is thrown and the retry operator is used
    if (stopwatch.isRunning()) {
      stopwatch.stop();
      stopwatch.reset();
    }
    stopwatch.start();
  }

  private void registerErrorMetrics(String uri, Throwable ex, Stopwatch stopwatch) {

    Integer maxAge = null;
    Integer status = null;
    if (ex instanceof HalApiClientException) {
      HalResponse response = ((HalApiClientException)ex).getErrorResponse();
      maxAge = response.getMaxAge();
      status = response.getStatus();
    }

    log.debug("Failed to receive JSON response from {} with status code {} and max-age {} in {}ms",
        uri, status, maxAge, stopwatch.elapsed(TimeUnit.MILLISECONDS));

    String title = "Upstream resource that failed to load: " + ex.getMessage();

    metrics.onResponseRetrieved(uri, title, maxAge, stopwatch.elapsed(TimeUnit.MICROSECONDS));
  }

  private void registerResponseMetrics(String uri, HalResponse response, Stopwatch stopwatch) {

    log.debug("Received JSON response from {} with status code {} and max-age {} in {}ms",
        uri, response.getStatus(), response.getMaxAge(), stopwatch.elapsed(TimeUnit.MILLISECONDS));

    String title = getResourceTitle(response.getBody(), uri);

    metrics.onResponseRetrieved(uri, title, response.getMaxAge(), stopwatch.elapsed(TimeUnit.MICROSECONDS));
  }

  private Single<HalResponse> rethrowUnexpectedExceptions(String uri, Throwable ex) {

    if (ex instanceof HalApiClientException) {
      return Single.error(ex);
    }

    RuntimeException re = new HalApiDeveloperException("An unexpected exception was emitted by " + delegate.getClass().getName() + " when requesting " + uri
        + ". Please make sure that your implementation rethrows all exceptions as HalApiClientException, to provide status code information whenever possible ",
        ex);
    return Single.error(re);
  }

  private static String getResourceTitle(HalResource halResource, String uri) {

    Link selfLink = halResource.getLink();

    String title = null;
    if (selfLink != null) {
      title = selfLink.getTitle();
    }

    if (title == null) {
      title = "Untitled HAL resource from " + uri;
    }

    return title;
  }
}
