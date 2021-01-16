/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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

import java.lang.reflect.Method;
import java.util.function.Function;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;

/**
 * An implementation of {@link HalApiTypeSupport} that delegates all method calls to the given
 * {@link HalApiAnnotationSupport} and {@link HalApiReturnTypeSupport} instances
 */
public class HalApiTypeSupportAdapter implements HalApiTypeSupport {

  private static final HalApiAnnotationSupport NO_ADDITIONAL_ANNOTATION_SUPPORT = new NoAdditionalAnnotationSupport();
  private static final HalApiReturnTypeSupport NO_ADDITIONAL_RETURN_TYPE_SUPPORT = new NoAdditionalReturnTypeSupport();

  private final HalApiAnnotationSupport annotationSupport;
  private final HalApiReturnTypeSupport returnTypeSupport;

  /**
   * @param annotationSupport to allow additional annotations
   */
  public HalApiTypeSupportAdapter(HalApiAnnotationSupport annotationSupport) {
    this.annotationSupport = annotationSupport;
    this.returnTypeSupport = NO_ADDITIONAL_RETURN_TYPE_SUPPORT;
  }

  /**
   * @param returnTypeSupport to allow additional return types
   */
  public HalApiTypeSupportAdapter(HalApiReturnTypeSupport returnTypeSupport) {
    this.annotationSupport = NO_ADDITIONAL_ANNOTATION_SUPPORT;
    this.returnTypeSupport = returnTypeSupport;
  }

  /**
   * @param annotationSupport optional extension to allow additional annotations
   * @param returnTypeSupport optional extension to allow additional return types
   */
  public HalApiTypeSupportAdapter(HalApiAnnotationSupport annotationSupport, HalApiReturnTypeSupport returnTypeSupport) {
    this.annotationSupport = annotationSupport != null ? annotationSupport : NO_ADDITIONAL_ANNOTATION_SUPPORT;
    this.returnTypeSupport = returnTypeSupport != null ? returnTypeSupport : NO_ADDITIONAL_RETURN_TYPE_SUPPORT;
  }

  @Override
  public boolean isHalApiInterface(Class<?> interfaze) {
    return annotationSupport.isHalApiInterface(interfaze);
  }

  @Override
  public String getContentType(Class<?> halApiInterface) {
    return annotationSupport.getContentType(halApiInterface);
  }

  @Override
  public boolean isResourceLinkMethod(Method method) {
    return annotationSupport.isResourceLinkMethod(method);
  }

  @Override
  public boolean isResourceRepresentationMethod(Method method) {
    return annotationSupport.isResourceRepresentationMethod(method);
  }

  @Override
  public boolean isRelatedResourceMethod(Method method) {
    return annotationSupport.isRelatedResourceMethod(method);
  }

  @Override
  public boolean isResourceStateMethod(Method method) {
    return annotationSupport.isResourceStateMethod(method);
  }

  @Override
  public String getRelation(Method method) {
    return annotationSupport.getRelation(method);
  }

  @Override
  public <T> Function<Observable, T> convertFromObservable(Class<T> targetType) {
    return returnTypeSupport.convertFromObservable(targetType);
  }

  @Override
  public Function<? super Object, Observable<?>> convertToObservable(Class<?> sourceType) {
    return returnTypeSupport.convertToObservable(sourceType);
  }

  HalApiAnnotationSupport getAnnotationSupport() {
    return annotationSupport;
  }

  HalApiReturnTypeSupport getReturnTypeSupport() {
    return returnTypeSupport;
  }

  static class NoAdditionalReturnTypeSupport implements HalApiReturnTypeSupport {

    @Override
    public <T> Function<Observable, T> convertFromObservable(Class<T> targetType) {
      return null;
    }

    @Override
    public Function<? super Object, Observable<?>> convertToObservable(Class<?> sourceType) {
      return null;
    }

  }

  static class NoAdditionalAnnotationSupport implements HalApiAnnotationSupport {

    @Override
    public boolean isHalApiInterface(Class<?> interfaze) {
      return false;
    }

    @Override
    public String getContentType(Class<?> halApiInterface) {
      return null;
    }

    @Override
    public boolean isResourceLinkMethod(Method method) {
      return false;
    }

    @Override
    public boolean isResourceRepresentationMethod(Method method) {
      return false;
    }

    @Override
    public boolean isRelatedResourceMethod(Method method) {
      return false;
    }

    @Override
    public boolean isResourceStateMethod(Method method) {
      return false;
    }

    @Override
    public String getRelation(Method method) {
      return null;
    }

  }
}
