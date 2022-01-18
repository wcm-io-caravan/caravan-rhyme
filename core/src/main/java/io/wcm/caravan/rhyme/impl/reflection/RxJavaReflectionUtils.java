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
package io.wcm.caravan.rhyme.impl.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import com.google.common.base.Preconditions;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;

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
  @SuppressWarnings("PMD.PreserveStackTrace")
  public static Observable<Object> invokeMethodAndReturnObservable(Object resourceImplInstance, Method method, RequestMetricsCollector metrics,
      HalApiTypeSupport typeSupport) {

    String fullMethodName = HalApiReflectionUtils.getClassAndMethodName(resourceImplInstance, method, typeSupport);

    try (RequestMetricsStopwatch sw = metrics.startStopwatch(AsyncHalResponseRenderer.class, () -> "calls to " + fullMethodName)) {

      Object[] args = new Object[method.getParameterCount()];
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
  }

  /**
   * @param method a method that returns a Observable
   * @return the type of the emitted results
   */
  public static Class<?> getObservableEmissionType(Method method, HalApiTypeSupport typeSupport) {
    Type returnType = method.getGenericReturnType();

    if (!(returnType instanceof ParameterizedType)) {
      return method.getReturnType();
    }

    ParameterizedType observableType = (ParameterizedType)returnType;

    Function<Object, Observable<Object>> conversion = typeSupport.convertToObservable(method.getReturnType());

    if (conversion == null) {
      throw new HalApiDeveloperException("The return type " + method.getReturnType().getSimpleName()
          + " of method " + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + " is not supported."
          + " If you do want to use this return type, you can add support for it by using " + RhymeBuilder.class.getSimpleName() + "#withReturnTypeSupport");
    }

    Type resourceType = observableType.getActualTypeArguments()[0];

    Preconditions.checkArgument(resourceType instanceof Class,
        "return types must be generic class with Class type parameters (e.g. List<ObjectNode>), but found " + resourceType.getTypeName());

    return (Class)resourceType;
  }

  /**
   * Converts the observable into any other type supported by the given {@link HalApiTypeSupport} instance
   * @param targetType to which the observable should be converted
   * @param observable to convert
   * @param typeSupport provides the available conversion functions
   * @return an object of the given target type
   */
  public static Object convertObservableTo(Class<?> targetType, Observable<?> observable, HalApiReturnTypeSupport typeSupport) {

    Preconditions.checkNotNull(targetType, "A target type must be provided");

    Function<Observable, ?> conversion = typeSupport.convertFromObservable(targetType);
    if (conversion == null) {
      throw new HalApiDeveloperException("The given target type of " + targetType.getName() + " is not a supported return type");
    }

    return conversion.apply(observable);
  }

  private static Observable<Object> convertToObservable(Object reactiveInstance, HalApiReturnTypeSupport typeSupport) {

    Preconditions.checkNotNull(reactiveInstance, "Cannot convert null objects");

    Function<Object, Observable<Object>> conversion = typeSupport.convertToObservable(reactiveInstance.getClass());
    if (conversion == null) {
      throw new HalApiDeveloperException("The given instance of " + reactiveInstance.getClass().getName() + " is not a supported return type");
    }

    return conversion.apply(reactiveInstance);
  }
}
