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
package io.wcm.caravan.reha.impl.reflection;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;

import io.reactivex.rxjava3.core.Observable;

/**
 * Combines the functionality of multiple {@link HalApiTypeSupport} instances
 */
public class CompositeHalApiTypeSupport implements HalApiTypeSupport {

  private final List<HalApiTypeSupport> delegates;

  /**
   * @param delegates the {@link HalApiTypeSupport} instances to combine
   */
  public CompositeHalApiTypeSupport(Iterable<HalApiTypeSupport> delegates) {

    this.delegates = ImmutableList.copyOf(delegates);
  }

  private boolean anyMatch(Predicate<? super HalApiTypeSupport> predicate) {

    return delegates.stream().anyMatch(predicate);
  }

  private <T> T firstNonNull(Function<? super HalApiTypeSupport, ? extends T> func) {

    return delegates.stream().map(func).filter(Objects::nonNull).findFirst().orElse(null);
  }

  @Override
  public boolean isHalApiInterface(Class<?> interfaze) {

    return anyMatch(delegate -> delegate.isHalApiInterface(interfaze));
  }

  @Override
  public String getContentType(Class<?> halApiInterface) {

    return firstNonNull(delegate -> delegate.getContentType(halApiInterface));
  }

  @Override
  public boolean isResourceLinkMethod(Method method) {

    return anyMatch(delegate -> delegate.isResourceLinkMethod(method));
  }

  @Override
  public boolean isResourceRepresentationMethod(Method method) {

    return anyMatch(delegate -> delegate.isResourceRepresentationMethod(method));
  }

  @Override
  public boolean isRelatedResourceMethod(Method method) {

    return anyMatch(delegate -> delegate.isRelatedResourceMethod(method));
  }

  @Override
  public boolean isResourceStateMethod(Method method) {

    return anyMatch(delegate -> delegate.isResourceStateMethod(method));
  }

  @Override
  public String getRelation(Method method) {

    return firstNonNull(delegate -> delegate.getRelation(method));
  }

  @Override
  public <T> Function<Observable, T> convertFromObservable(Class<T> targetType) {

    return firstNonNull(delegate -> delegate.convertFromObservable(targetType));
  }

  @Override
  public Function<? super Object, Observable<?>> convertToObservable(Class<?> sourceType) {

    return firstNonNull(delegate -> delegate.convertToObservable(sourceType));
  }

  List<HalApiTypeSupport> getDelegates() {
    return delegates;
  }
}
