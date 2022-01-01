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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.server.ResourceConversions;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;

/**
 * Utility methods to inspect method signatures
 */
public final class HalApiReflectionUtils {

  private HalApiReflectionUtils() {
    // static methods only
  }

  static Set<Class<?>> collectInterfaces(Class clazz) {

    return new InterfaceCollector()
        .collectFromClassAndAllSuperClasses(clazz);
  }

  private static final class InterfaceCollector {

    private final Set<Class<?>> collected = new LinkedHashSet<>();

    Set<Class<?>> collectFromClassAndAllSuperClasses(Class clazz) {

      Class c = clazz;
      while (c != null) {
        addInterfacesImplementedBy(c);
        c = c.getSuperclass();
      }

      return collected;
    }

    private void addInterfacesImplementedBy(Class<?> clazz) {
      ArrayList<Class<?>> interfaces = Lists.newArrayList(clazz.getInterfaces());

      interfaces.forEach(this.collected::add);

      // each interface can extend other interfaces
      interfaces.forEach(this::addInterfacesImplementedBy);
    }
  }

  /**
   * Checks which of the interfaces implemented by the given implementation instance is the one which is annotated with
   * {@link HalApiInterface}
   * @param resourceImplInstance an instance of a class implementing a HAL API interface
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return the interface that is annotated with {@link HalApiInterface}
   */
  public static Class<?> findHalApiInterface(Object resourceImplInstance, HalApiAnnotationSupport annotationSupport) {

    Class<?> halApiInterface = collectInterfaces(resourceImplInstance.getClass()).stream()
        .filter(annotationSupport::isHalApiInterface)
        .findFirst()
        .orElseThrow(
            () -> new HalApiDeveloperException(
                "None of the interfaces implemented by the given class " + resourceImplInstance.getClass().getName() + " has a @"
                    + HalApiInterface.class.getSimpleName() + " annotation"));

    if (!Modifier.isPublic(halApiInterface.getModifiers())) {
      throw new HalApiDeveloperException(
          "The interface " + halApiInterface.getName() + " is annotated with @HalApiInterface but it also has to be public");
    }

    return halApiInterface;
  }

  /**
   * @param relatedResourceType an interface used as emission type of a reactive stream
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return true if the given interface is (or extends another interface) annotated with {@link HalApiInterface}
   */
  public static boolean isHalApiInterface(Class<?> relatedResourceType, HalApiAnnotationSupport annotationSupport) {

    if (!relatedResourceType.isInterface()) {
      return false;
    }

    if (annotationSupport.isHalApiInterface(relatedResourceType)) {
      return true;
    }

    return collectInterfaces(relatedResourceType).stream()
        .anyMatch(annotationSupport::isHalApiInterface);
  }

  /**
   * @param apiInterface an interface annotated with {@link HalApiInterface} (either directly or by extending)
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return the method annotated with {@link ResourceState}
   */
  public static Optional<Method> findResourceStateMethod(Class<?> apiInterface, HalApiAnnotationSupport annotationSupport) {

    return Stream.of(apiInterface.getMethods())
        .filter(annotationSupport::isResourceStateMethod)
        .findFirst();
  }

  /**
   * @param apiInterface an interface annotated with {@link HalApiInterface} (either directly or by extending)
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return the method annotated with {@link ResourceState}
   */
  public static List<Method> findResourcePropertyMethods(Class<?> apiInterface, HalApiAnnotationSupport annotationSupport) {

    return Stream.of(apiInterface.getMethods())
        .filter(annotationSupport::isResourcePropertyMethod)
        .collect(Collectors.toList());
  }

  /**
   * @param apiInterface an interface annotated with {@link HalApiInterface} (either directly or by extending)
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return a list of all methods annotated with {@link Related}
   */
  public static List<Method> getSortedRelatedResourceMethods(Class<?> apiInterface, HalApiAnnotationSupport annotationSupport) {

    MethodRelationComparator comparator = new MethodRelationComparator(annotationSupport);

    return Stream.of(apiInterface.getMethods())
        .filter(annotationSupport::isRelatedResourceMethod)
        .sorted(comparator)
        .collect(Collectors.toList());
  }

  /**
   * @param resourceImplInstance an implementation of a HAL API interface
   * @param annotationSupport to detect annotated methods
   * @return a name of the class to be used for logging and embedded metadata
   */
  public static String getSimpleClassName(Object resourceImplInstance, HalApiAnnotationSupport annotationSupport) {

    Class<?> implClass = resourceImplInstance.getClass();

    if (!implClass.isAnonymousClass()) {
      return implClass.getSimpleName();
    }

    try {
      Class<?> apiInterface = findHalApiInterface(resourceImplInstance, annotationSupport);

      return "anonymous " + apiInterface.getSimpleName() + " (defined in " + implClass.getEnclosingClass().getSimpleName() + ")";
    }
    catch (HalApiDeveloperException ex) {
      return implClass.getName();
    }
  }

  /**
   * @param instance of any object
   * @param method a method of this class
   * @param annotationSupport the strategy to detect HAL API annotations
   * @return a string with the simple class name and method name
   */
  public static String getClassAndMethodName(Object instance, Method method, HalApiAnnotationSupport annotationSupport) {

    String simpleClassName = getSimpleClassName(instance, annotationSupport);
    String methodName = method.getName();

    return "#" + methodName + " of " + simpleClassName;
  }

  private static final class MethodRelationComparator implements Comparator<Method> {

    private final HalApiAnnotationSupport annotationSupport;

    private MethodRelationComparator(HalApiAnnotationSupport annotationSupport) {
      this.annotationSupport = annotationSupport;
    }

    @Override
    public int compare(Method method1, Method method2) {
      String curi1 = annotationSupport.getRelation(method1);
      String curi2 = annotationSupport.getRelation(method2);

      // make sure that all standard link relations always come before custom relations
      if (curi2.contains(":") && !curi1.contains(":")) {
        return -1;
      }
      if (curi1.contains(":") && !curi2.contains(":")) {
        return 1;
      }

      // otherwise the links should be sorted alphabetically
      return curi1.compareTo(curi2);
    }

  }

  @SuppressWarnings("unchecked")
  public static <T> T createEmbeddedResourceProxy(T linkableResource, boolean linkedWhenEmbedded) {

    Set<Class<?>> interfaces = collectInterfaces(linkableResource.getClass());

    interfaces.add(EmbeddableResource.class);

    return (T)Proxy.newProxyInstance(linkableResource.getClass().getClassLoader(),
        interfaces.stream().toArray(Class[]::new),
        new EmbeddedResourceProxyInvocationHandler<T>(linkableResource, linkedWhenEmbedded));
  }

  static final class EmbeddedResourceProxyInvocationHandler<T> implements InvocationHandler {

    private final T linkableResource;
    private final boolean linkedWhenEmbedded;

    EmbeddedResourceProxyInvocationHandler(T linkableResource, boolean linkedWhenEmbedded) {
      this.linkableResource = linkableResource;
      this.linkedWhenEmbedded = linkedWhenEmbedded;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {

      try {
        if ("isEmbedded".equals(method.getName())) {
          return true;
        }
        if ("isLinkedWhenEmbedded".equals(method.getName())) {
          return this.linkedWhenEmbedded;
        }

        return method.invoke(this.linkableResource, args);
      }
      catch (IllegalAccessException | InvocationTargetException | RuntimeException ex) {

        // if the original implementation method just threw a runtime exception then re-throw that cause
        if (ex instanceof InvocationTargetException) {
          if (ex.getCause() instanceof RuntimeException) {
            throw (RuntimeException)ex.getCause();
          }
        }

        throw new HalApiServerException(500,
            "Failed to invoke method " + method.getName() + " on proxy created with class "
                + ResourceConversions.class.getSimpleName(),
            ex);
      }

    }
  }

}
