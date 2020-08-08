/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.reha.impl.client;

import static io.wcm.caravan.reha.impl.reflection.HalApiReflectionUtils.isHalApiInterface;

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
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.ResourceLink;
import io.wcm.caravan.reha.api.client.HalApiClient;
import io.wcm.caravan.reha.api.client.HalApiDeveloperException;
import io.wcm.caravan.reha.api.client.JsonResourceLoader;
import io.wcm.caravan.reha.api.common.HalApiTypeSupport;
import io.wcm.caravan.reha.api.common.HalResponse;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.impl.metadata.EmissionStopwatch;

/**
 * Contains static factory methods to create proxy implementations of a given interface annotated with
 * {@link HalApiInterface}
 */
final class HalApiClientProxyFactory {


  private final Cache<String, Object> proxyCache = CacheBuilder.newBuilder().build();

  private final JsonResourceLoader jsonLoader;
  private final RequestMetricsCollector metrics;
  private final HalApiTypeSupport typeSupport;

  HalApiClientProxyFactory(JsonResourceLoader jsonLoader, RequestMetricsCollector metrics, HalApiTypeSupport typeSupport) {
    this.metrics = metrics;
    this.jsonLoader = jsonLoader;
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

          return jsonLoader.loadJsonResource(url)
              .map(HalResponse::getBody);
        })
        .compose(EmissionStopwatch.collectMetrics("fetching " + relatedResourceType.getSimpleName() + " from upstream server", metrics));
  }


  @SuppressWarnings("unchecked")
  private <T> T getProxy(Class<T> relatedResourceType, Single<HalResource> rxHal, Link linkToResource) {

    if (linkToResource == null) {
      return createProxy(relatedResourceType, rxHal, linkToResource);
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
    return interfaces.toArray(new Class[interfaces.size()]);
  }
}
