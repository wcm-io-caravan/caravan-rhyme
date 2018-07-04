/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.api.server.impl.reflection;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
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

  static Comparator<Method> methodRelationComparator = (method1, method2) -> {
    String curi1 = method1.getAnnotation(RelatedResource.class).relation();
    String curi2 = method2.getAnnotation(RelatedResource.class).relation();

    // make sure that all links with custom link relations are displayed first
    if (curi1.contains(":") && !curi2.contains(":")) {
      return -1;
    }
    // make sure that all links with standard relations are displayed last
    if (curi2.contains(":") && !curi1.contains(":")) {
      return 1;
    }
    // otherwise the links should be sorted alphabetically
    return curi1.compareTo(curi2);
  };

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

  public static Observable<Method> getSortedRelatedResourceMethods(Class<?> resourceInterface) {

    return Observable.from(resourceInterface.getMethods())
        .filter(method -> method.getAnnotation(RelatedResource.class) != null)
        .toSortedList((m1, m2) -> HalApiReflectionUtils.methodRelationComparator.compare(m1, m2))
        .flatMapIterable(l -> l);
  }
}
