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

import static io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils.isHalApiInterface;

import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceLink;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.metadata.EmissionStopwatch;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;

/**
 * Contains static factory methods to create proxy implementations of a given interface annotated with
 * {@link HalApiInterface}
 */
final class HalApiClientProxyFactory {


  private final Cache<String, Object> proxyCache = CacheBuilder.newBuilder().build();

  private final HalResourceLoader resourceLoader;
  private final RequestMetricsCollector metrics;
  private final HalApiTypeSupport typeSupport;

  HalApiClientProxyFactory(HalResourceLoader resourceLoader, RequestMetricsCollector metrics, HalApiTypeSupport typeSupport) {
    this.metrics = metrics;
    this.resourceLoader = resourceLoader;
    this.typeSupport = typeSupport;
  }

  <T> T createProxyFromUrl(Class<T> relatedResourceType, String url) {

    Single<HalResource> rxHal = loadHalResource(url, relatedResourceType);

    return getProxy(relatedResourceType, rxHal, new Link(url));
  }

  <T> T createProxyFromLink(Class<T> relatedResourceType, Link link) {

    Single<HalResource> rxHal = loadHalResource(link.getHref(), relatedResourceType);

    return getProxy(relatedResourceType, rxHal, link);
  }

  <T> T createProxyFromHalResource(Class<T> relatedResourceType, HalResource contextResource, Link link) {

    Single<HalResource> rxHal = Single.just(contextResource);

    return getProxy(relatedResourceType, rxHal, link);
  }

  private <T> Single<HalResource> loadHalResource(String resourceUrl, Class<T> relatedResourceType) {

    // this additional single is only required because we want to validate the URL only on subscription
    // (e.g. right before it is actually retrieved).
    // This is because i should still be possible to create a proxy just to get a URI template
    // by calling a method annotated with @ResourceLink.
    return Single.just(resourceUrl)
        .flatMap(url -> {
          Link link = new Link(url);
          if (link.isTemplated()) {
            throw new HalApiDeveloperException("Cannot follow the link template to " + link.getHref()
                + " because it has not been expanded."
                + " If you are calling a proxy method with parameters then make sure to provide at least one parameter "
                + "(unless you are only interested in obtaining the link template by calling the method annotated with @" + ResourceLink.class.getSimpleName()
                + ")");
          }

          return resourceLoader.getHalResource(url)
              .map(HalResponse::getBody);
        })
        .compose(EmissionStopwatch.collectMetrics("fetching " + relatedResourceType.getSimpleName() + " from upstream server", metrics));
  }


  @SuppressWarnings("unchecked")
  private <T> T getProxy(Class<T> relatedResourceType, Single<HalResource> rxHal, Link linkToResource) {

    // do not try to cache proxies for resources for which no link is available
    if (linkToResource == null) {
      return createProxy(relatedResourceType, rxHal, null);
    }

    String cacheKey = linkToResource.getModel().toString() + relatedResourceType.getName();

    try {
      return (T)proxyCache.get(cacheKey, () -> createProxy(relatedResourceType, rxHal, linkToResource));
    }
    catch (UncheckedExecutionException | ExecutionException ex) {
      // we not that createProxy never throws any checked exception, so its safe to case to re-throw the original exception
      throw (RuntimeException)ex.getCause();
    }
  }

  private <T> T createProxy(Class<T> relatedResourceType, Single<HalResource> rxHal, Link linkToResource) {

    Stopwatch sw = Stopwatch.createStarted();

    try {
      // check that the given class is indeed a HAL api interface
      if (!isHalApiInterface(relatedResourceType, typeSupport)) {
        throw new HalApiDeveloperException(
            "The given resource interface " + relatedResourceType.getName() + " does not have a @" + HalApiInterface.class.getSimpleName() + " annotation.");
      }

      Class[] interfaces = getInterfacesToImplement(relatedResourceType);

      // the main logic of the proxy is implemented in this InvocationHandler
      HalApiInvocationHandler invocationHandler = new HalApiInvocationHandler(rxHal, relatedResourceType, linkToResource, this, metrics, typeSupport);

      @SuppressWarnings("unchecked")
      T proxy = (T)Proxy.newProxyInstance(relatedResourceType.getClassLoader(), interfaces, invocationHandler);

      return proxy;
    }
    finally {
      metrics.onMethodInvocationFinished(HalApiClient.class,
          "creating " + relatedResourceType.getSimpleName() + " proxy instance",
          sw.elapsed(TimeUnit.MICROSECONDS));
    }
  }

  private <T> Class[] getInterfacesToImplement(Class<T> relatedResourceType) {
    List<Class> interfaces = new LinkedList<>();
    interfaces.add(relatedResourceType);
    return interfaces.toArray(new Class[0]);
  }
}
