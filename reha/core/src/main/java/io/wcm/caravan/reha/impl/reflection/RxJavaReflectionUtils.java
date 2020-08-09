/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.reha.impl.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.reha.api.client.HalApiDeveloperException;
import io.wcm.caravan.reha.api.common.HalApiReturnTypeSupport;
import io.wcm.caravan.reha.api.common.HalApiTypeSupport;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.server.AsyncHalResourceRenderer;
import io.wcm.caravan.reha.api.server.HalApiServerException;
import io.wcm.caravan.reha.impl.metadata.EmissionStopwatch;
import io.wcm.caravan.reha.util.RxJavaTransformers;

/**
 * Internal utility methods to invoke methods returning reactive streams, and converting between various
 * reactive types.
 */
public final class RxJavaReflectionUtils {

  private RxJavaReflectionUtils() {
    // this class contains only static methods
  }

  /**
   * @param resourceImplInstance the object on which to invoke the method
   * @param method a method that returns a {@link Single}, {@link Maybe}, {@link Observable} or {@link Publisher}
   * @param metrics to track the method invocation time
   * @param typeSupport the strategy to detect HAL API annotations and perform type conversions
   * @return an {@link Observable} that emits the items from the reactive stream returned by the method
   */
  public static Observable<?> invokeMethodAndReturnObservable(Object resourceImplInstance, Method method, RequestMetricsCollector metrics,
      HalApiTypeSupport typeSupport) {

    Stopwatch stopwatch = Stopwatch.createStarted();

    String fullMethodName = HalApiReflectionUtils.getClassAndMethodName(resourceImplInstance, method, typeSupport);

    Object[] args = new Object[method.getParameterCount()];

    try {
      Object returnValue = method.invoke(resourceImplInstance, args);

      if (returnValue == null) {
        throw new HalApiDeveloperException(
            fullMethodName + " must not return null. You should return an empty Maybe/Observable if the related resource does not exist");
      }

      return convertToObservable(returnValue, typeSupport);
    }
    catch (InvocationTargetException ex) {
      Throwable cause = ex.getTargetException();
      if (cause instanceof RuntimeException) {
        throw ((RuntimeException)cause);
      }
      throw new HalApiServerException(500, "A checked exception was thrown when calling " + fullMethodName, cause);
    }
    catch (IllegalAccessException | IllegalArgumentException ex) {
      throw new HalApiDeveloperException("Failed to invoke method " + fullMethodName, ex);
    }
    finally {

      metrics.onMethodInvocationFinished(AsyncHalResourceRenderer.class,
          "calling " + fullMethodName,
          stopwatch.elapsed(TimeUnit.MICROSECONDS));
    }
  }

  /**
   * @param method a method that returns a Observable
   * @return the type of the emitted results
   */
  public static Class<?> getObservableEmissionType(Method method) {
    Type returnType = method.getGenericReturnType();

    if (!(returnType instanceof ParameterizedType)) {
      return method.getReturnType();
    }

    ParameterizedType observableType = (ParameterizedType)returnType;

    Type resourceType = observableType.getActualTypeArguments()[0];

    Preconditions.checkArgument(resourceType instanceof Class,
        "return types must be generic class with Class type parameters (e.g. List<ObjectNode>), but found " + resourceType.getTypeName());

    return (Class)resourceType;
  }

  /**
   * @param reactiveInstance a {@link Single}, {@link Maybe}, {@link Observable} or {@link Publisher}
   * @param targetType {@link Single}, {@link Maybe}, {@link Observable} or {@link Publisher} class
   * @param metrics to collect emission times
   * @param description for the metrics
   * @param typeSupport the strategy to perform type conversions of return values
   * @return an instance of the target type that will replay (and cache!) the items emitted by the given reactive
   *         instance
   */
  public static Object convertAndCacheReactiveType(Object reactiveInstance, Class<?> targetType, RequestMetricsCollector metrics, String description,
      HalApiReturnTypeSupport typeSupport) {

    Observable<?> observable = convertToObservable(reactiveInstance, typeSupport)
        .compose(EmissionStopwatch.collectMetrics(description, metrics));

    // do not use Observable#cache() here, because we want consumers to be able to use Observable#retry()
    Observable<?> cached = observable.compose(RxJavaTransformers.cacheIfCompleted());

    return convertObservableTo(cached, targetType, typeSupport);
  }

  private static Object convertObservableTo(Observable<?> observable, Class<?> targetType, HalApiReturnTypeSupport typeSupport) {

    Preconditions.checkNotNull(targetType, "A target type must be provided");

    Function<Observable, ?> conversion = typeSupport.convertFromObservable(targetType);
    if (conversion == null) {
      throw new HalApiDeveloperException("The given target type of " + targetType.getName() + " is not a supported return type");
    }

    return conversion.apply(observable);
  }

  private static Observable<?> convertToObservable(Object reactiveInstance, HalApiReturnTypeSupport typeSupport) {

    Preconditions.checkNotNull(reactiveInstance, "Cannot convert null objects");

    Function<Object, Observable<?>> conversion = typeSupport.convertToObservable(reactiveInstance.getClass());
    if (conversion == null) {
      throw new HalApiDeveloperException("The given instance of " + reactiveInstance.getClass().getName() + " is not a supported return type");
    }

    Observable<?> observable = conversion.apply(reactiveInstance);

    return observable;

  }
}
