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

package io.wcm.caravan.rhyme.impl.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.RxJavaReflectionUtils;

/**
 * Handles calls to proxy methods from dynamic proxies created with {@link HalApiClientProxyFactory}
 */
final class HalApiInvocationHandler implements InvocationHandler {


  private final Cache<String, Observable> returnValueCache = CacheBuilder.newBuilder().build();

  private final Single<HalResource> rxResource;
  private final Class resourceInterface;
  private final Link linkToResource;
  private final HalApiClientProxyFactory proxyFactory;
  private final RequestMetricsCollector metrics;
  private final HalApiTypeSupport typeSupport;

  HalApiInvocationHandler(Single<HalResource> rxResource, Class resourceInterface, Link linkToResource,
      HalApiClientProxyFactory proxyFactory, RequestMetricsCollector metrics, HalApiTypeSupport typeSupport) {

    this.rxResource = rxResource;
    this.resourceInterface = resourceInterface;
    this.linkToResource = linkToResource;
    this.proxyFactory = proxyFactory;
    this.metrics = metrics;
    this.typeSupport = typeSupport;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    // we want to measure how much time is spent for reflection magic in this proxy
    Stopwatch stopwatch = Stopwatch.createStarted();

    // create an object to help with identification of methods and parameters
    HalApiMethodInvocation invocation = new HalApiMethodInvocation(resourceInterface, method, args, typeSupport);

    try {
      Observable rxReturnValue = getCachedObservableReturnValue(invocation);
      return RxJavaReflectionUtils.convertObservableTo(rxReturnValue, method.getReturnType(), typeSupport);
    }
    // CHECKSTYLE:OFF
    catch (HalApiDeveloperException | HalApiClientException ex) {
      // these exceptions should just be re-thrown as they are implementation errors by the developer
      // (e.g. using invalid types in the signatures of the HAL API interface),
      //or contain important context information from failed HTTP request
      throw ex;
    }
    // CHECKSTYLE:ON
    catch (NoSuchElementException ex) {
      // these exceptions should be re-thrown with a better error message
      throw new HalApiDeveloperException("The invocation of " + invocation + " has failed, "
          + "most likely because no link or embedded resource with appropriate relation was found in the HAL resource", ex);
    }
    // CHECKSTYLE:OFF - we really want to catch any other possible runtime exceptions here to add additional information on the method being called
    catch (Exception e) {
      // CHECKSTYLE:ON
      throw new RuntimeException("The invocation of " + invocation + " has failed with an unexpected exception", e);
    }
    finally {
      // collect the time spend calling all proxy methods during the current request in the HalResponseMetadata object
      metrics.onMethodInvocationFinished(HalApiClient.class, "calling " + invocation.toString(), stopwatch.elapsed(TimeUnit.MICROSECONDS));
    }
  }

  private Observable getCachedObservableReturnValue(HalApiMethodInvocation invocation) throws ExecutionException {
    try {
      return returnValueCache.get(invocation.getCacheKey(), () -> callAnnotationSpecificHandler(invocation));
    }
    catch (UncheckedExecutionException ex) {
      throw (RuntimeException)ex.getCause();
    }
  }

  private Single<HalResource> addContextToHalApiClientException(Throwable ex, HalApiMethodInvocation invocation) {
    if (ex instanceof HalApiClientException) {
      String msg = "Failed to load an upstream resource that was requested by calling " + invocation;
      return Single.error(new HalApiClientException(msg, (HalApiClientException)ex));
    }
    return Single.error(ex);
  }

  @SuppressWarnings("PMD.AvoidRethrowingException")
  private Observable callAnnotationSpecificHandler(HalApiMethodInvocation invocation) {

    if (invocation.isForMethodAnnotatedWithResourceState()) {

      Maybe<Object> state = rxResource
          .onErrorResumeNext(ex -> addContextToHalApiClientException(ex, invocation))
          .map(hal -> new ResourceStateHandler(hal))
          .flatMapMaybe(handler -> handler.handleMethodInvocation(invocation));

      return RxJavaReflectionUtils.convertAndCacheReactiveType(state, invocation.getReturnType(), metrics, invocation.getDescription(), typeSupport);
    }

    if (invocation.isForMethodAnnotatedWithRelatedResource()) {

      Observable<Object> relatedProxies = rxResource
          .onErrorResumeNext(ex -> addContextToHalApiClientException(ex, invocation))
          .map(hal -> new RelatedResourceHandler(hal, proxyFactory, typeSupport))
          .flatMapObservable(handler -> handler.handleMethodInvocation(invocation));

      return RxJavaReflectionUtils.convertAndCacheReactiveType(relatedProxies, invocation.getReturnType(), metrics, invocation.getDescription(), typeSupport);
    }

    if (invocation.isForMethodAnnotatedWithResourceLink()) {

      ResourceLinkHandler handler = new ResourceLinkHandler(linkToResource);
      return Observable.just(handler.handleMethodInvocation(invocation));
    }

    if (invocation.isForMethodAnnotatedWithResourceRepresentation()) {

      Single<Object> representation = rxResource
          .onErrorResumeNext(ex -> addContextToHalApiClientException(ex, invocation))
          .map(hal -> new ResourceRepresentationHandler(hal))
          .flatMap(handler -> handler.handleMethodInvocation(invocation));

      return RxJavaReflectionUtils.convertAndCacheReactiveType(representation, invocation.getReturnType(), metrics, invocation.getDescription(), typeSupport);
    }

    if (invocation.toString().endsWith("#toString()")) {
      String linkDesc = linkToResource != null ? " at " + linkToResource.getHref() : " (embedded without self link)";
      return Observable.just("dynamic client proxy for " + invocation.getResourceInterfaceName() + linkDesc);
    }

    // unsupported operation
    throw new HalApiDeveloperException("The method " + invocation + " is not annotated with one of the supported HAL API annotations");
  }
}
