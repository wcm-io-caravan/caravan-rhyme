/*-
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 - 2020 wcm.io Caravan
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

package io.wcm.caravan.rhyme.impl.client.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.impl.metadata.EmissionStopwatch;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.rhyme.impl.util.RxJavaTransformers;

/**
 * Handles calls to proxy methods from dynamic proxies created with {@link HalApiClientProxyFactory}
 */
final class HalApiInvocationHandler implements InvocationHandler {

  private final Cache<String, Observable<Object>> returnValueCache = CacheBuilder.newBuilder().build();

  private final Single<HalResource> rxResource;
  private final Class resourceInterface;
  private final Link linkToResource;

  private final HalApiClientProxyFactory proxyFactory;
  private final RequestMetricsCollector metrics;
  private final HalApiTypeSupport typeSupport;
  private final ObjectMapper objectMapper;

  HalApiInvocationHandler(Single<HalResource> rxResource, Class resourceInterface, Link linkToResource,
      HalApiClientProxyFactory proxyFactory, RequestMetricsCollector metrics, HalApiTypeSupport typeSupport, ObjectMapper objectMapper) {

    this.rxResource = rxResource;
    this.resourceInterface = resourceInterface;
    this.linkToResource = linkToResource;
    this.proxyFactory = proxyFactory;
    this.metrics = metrics;
    this.typeSupport = typeSupport;
    this.objectMapper = objectMapper;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    if ("hashCode".equals(method.getName())) {
      throw new HalApiDeveloperException("You cannot call hashCode() on dynamic client proxies. Avoid using collections like LinkedHashSet.");
    }

    // create an object to help with identification of methods and parameters
    HalApiMethodInvocation invocation = new HalApiMethodInvocation(metrics, resourceInterface, method, args, typeSupport);

    // collect the time spend calling all proxy methods during the current request in the HalResponseMetadata object
    try (RequestMetricsStopwatch sw = metrics.startStopwatch(HalApiClient.class, () -> "calling " + invocation)) {

      Observable<Object> rxReturnValue = getCachingObservableReturnValue(invocation);

      return RxJavaReflectionUtils.convertObservableTo(rxReturnValue, method.getReturnType(), typeSupport);
    }
    // CHECKSTYLE:OFF
    catch (HalApiDeveloperException | HalApiClientException ex) {
      // these exceptions should just be re-thrown as they are either implementation errors by the developer
      // (e.g. using invalid types in the signatures of the HAL API interface), or client errors
      // which both already contain important context information
      throw ex;
    }
    // CHECKSTYLE:ON
    catch (NoSuchElementException ex) {
      // these exceptions should be re-thrown with a better error message
      throw new HalApiDeveloperException("The invocation of " + invocation + " has failed, "
          + "most likely because no link or embedded resource with the appropriate relation was found in the HAL resource", ex);
    }
    // CHECKSTYLE:OFF - we really want to catch any other possible runtime exceptions here to add additional information on the method being called
    catch (Exception e) {
      // CHECKSTYLE:ON
      throw new HalApiDeveloperException("The invocation of " + invocation + " on a client proxy has failed with an unexpected exception", e);
    }
  }

  private Observable<Object> getCachingObservableReturnValue(HalApiMethodInvocation invocation) throws ExecutionException {
    try {
      return returnValueCache.get(invocation.getCacheKey(), () -> callAnnotationSpecificHandler(invocation));
    }
    catch (UncheckedExecutionException ex) {
      throw (RuntimeException)ex.getCause();
    }
  }

  private Observable<Object> callAnnotationSpecificHandler(HalApiMethodInvocation invocation) {

    // first handle the calls for methods that don't need the HAL resource to be loaded

    if (invocation.isForMethodAnnotatedWithResourceLink()) {

      ResourceLinkHandler handler = new ResourceLinkHandler(linkToResource);
      return Observable.just(handler.handleMethodInvocation(invocation));
    }

    if (invocation.toString().endsWith("#toString()")) {

      String linkDesc = linkToResource != null ? " at " + linkToResource.getHref() : " (embedded without self link)";
      return Observable.just("dynamic client proxy for " + invocation.getResourceInterfaceName() + linkDesc);
    }

    // create the right handler depending on the annotation on the method
    Function<HalResource, Observable<Object>> handler = createAnnotationSpecificHandler(invocation);

    // load the resource
    return rxResource
        // once it's loaded, create the method's return value as an Observable
        .flatMapObservable(handler::apply)
        // add context information if the client failed to load the resource
        .onErrorResumeNext(ex -> addContextToHalApiClientException(ex, invocation))
        // measure the time it takes for all this to complete
        .compose(EmissionStopwatch.collectMetrics(invocation::getDescription, metrics))
        // ensure that the Observable can be replayed if there are multiple invocations of the same proxy method,
        // but not use Observable#cache() here, because we want consumers to be able to use Observable#retry()
        .compose(RxJavaTransformers.cacheIfCompleted());
  }

  private Function<HalResource, Observable<Object>> createAnnotationSpecificHandler(HalApiMethodInvocation invocation) {

    if (invocation.isForMethodAnnotatedWithResourceState()) {

      return new ResourceStateHandler(invocation, typeSupport, objectMapper);
    }

    if (invocation.isForMethodAnnotatedWithResourceProperties()) {

      return new ResourcePropertyHandler(invocation, typeSupport, objectMapper);
    }

    if (invocation.isForMethodAnnotatedWithRelatedResource()) {

      return new RelatedResourceHandler(invocation, typeSupport, proxyFactory);
    }

    if (invocation.isForMethodAnnotatedWithResourceRepresentation()) {

      return new ResourceRepresentationHandler(invocation);
    }

    // unsupported operation
    throw new HalApiDeveloperException("The method " + invocation + " is not annotated with one of the supported HAL API annotations");
  }

  private Observable<HalResource> addContextToHalApiClientException(Throwable ex, HalApiMethodInvocation invocation) {

    if (ex instanceof HalApiClientException) {
      String msg = "Failed to load an upstream resource that was requested by calling " + invocation;
      return Observable.error(new HalApiClientException(msg, (HalApiClientException)ex));
    }
    return Observable.error(ex);
  }
}
