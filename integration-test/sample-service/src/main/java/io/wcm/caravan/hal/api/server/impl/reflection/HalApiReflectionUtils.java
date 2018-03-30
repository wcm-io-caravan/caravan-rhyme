/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.api.server.impl.reflection;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import rx.Observable;
import rx.Single;

/**
 * Utility methods to inspect method signatures
 */
public final class HalApiReflectionUtils {

  private HalApiReflectionUtils() {
    // static methods only
  }

  static Set<Class<?>> collectInterfaces(Class clazz) {

    Set<Class<?>> interfaces = new HashSet<>();

    Consumer<Class<?>> addInterfaces = new Consumer<Class<?>>() {

      @Override
      public void accept(Class<?> c) {
        for (Class interfaze : c.getInterfaces()) {
          interfaces.add(interfaze);
          accept(interfaze);
        }

      }
    };

    Class c = clazz;
    do {
      addInterfaces.accept(c);
      c = c.getSuperclass();
    }
    while (c != null);

    return interfaces;
  }

  public static Class<?> findHalApiInterface(Object resourceImplInstance) {

    return collectInterfaces(resourceImplInstance.getClass()).stream()
        .filter(interfaze -> interfaze.getAnnotation(HalApiInterface.class) != null)
        .findFirst()
        .orElseThrow(
            () -> new RuntimeException("None of the interfaces implemented by the given class " + resourceImplInstance.getClass().getName() + " has a @"
                + HalApiInterface.class.getSimpleName() + " annotation"));
  }

  public static Single<?> getResourceStateObservable(Class<?> resourceInterface, Object instance) {

    // find the first method annotated with @ResourceState
    Observable<Object> rxResourceState = Observable.from(resourceInterface.getMethods())
        .filter(method -> method.getAnnotation(ResourceState.class) != null)
        .limit(1)
        // invoke the method to get the state object and re-throw any exceptions that might be thrown
        .flatMap(method -> RxJavaReflectionUtils.invokeMethodAndReturnObservable(instance, method));

    // use an empty JSON object if no method is annotated with @ResourceState (or if the instance returned null)
    return rxResourceState
        .filter(Objects::nonNull)
        .defaultIfEmpty(JsonNodeFactory.instance.objectNode())
        .toSingle();
  }
}
