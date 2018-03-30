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
package io.wcm.caravan.hal.api.server.impl.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.common.base.Preconditions;

import rx.Observable;
import rx.Single;

public class RxJavaReflectionUtils {

  public static Observable<?> invokeMethodAndReturnObservable(Object resourceImplInstance, Method method) {

    String fullMethodName = resourceImplInstance.getClass() + "#" + method.getName();

    Object[] args = new Object[method.getParameterCount()];

    try {
      Object returnValue = method.invoke(resourceImplInstance, args);

      if (returnValue == null) {
        return Observable.empty();
      }

      if (returnValue instanceof Observable) {
        return (Observable<?>)returnValue;
      }

      if (returnValue instanceof Single) {
        return ((Single<?>)returnValue).toObservable();
      }

      throw new RuntimeException("The method " + fullMethodName + " returned an objected type "
          + returnValue.getClass().getName());
    }
    catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
      throw new RuntimeException("Failed to invoke method " + fullMethodName, ex);
    }
  }

  /**
   * @param method a method that returns a Observable
   * @return the type of the emitted results
   */
  public static Class<?> getObservableEmissionType(Method method) {
    Type returnType = method.getGenericReturnType();

    Preconditions.checkArgument(returnType instanceof ParameterizedType,
        "return types must be Observable<Class>, but " + method + " has a return type " + returnType.getTypeName());

    ParameterizedType observableType = (ParameterizedType)returnType;

    Type resourceType = observableType.getActualTypeArguments()[0];

    Preconditions.checkArgument(resourceType instanceof Class, "return types must be Observables of Class type, but found " + resourceType.getTypeName());

    return (Class)resourceType;
  }

  /**
   * @param method the method to check
   * @return true if this method returns a {@link Observable}
   */
  public static boolean hasObservableReturnType(Method method) {

    Class returnType = method.getReturnType();

    return Observable.class.isAssignableFrom(returnType);
  }

}
