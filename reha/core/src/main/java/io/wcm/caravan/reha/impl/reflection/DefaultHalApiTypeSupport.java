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
import java.util.Optional;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import com.google.common.collect.ImmutableList;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.annotations.ResourceLink;
import io.wcm.caravan.reha.api.annotations.ResourceRepresentation;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.common.HalApiAnnotationSupport;
import io.wcm.caravan.reha.api.common.HalApiReturnTypeSupport;
import io.wcm.caravan.reha.api.common.HalApiTypeSupport;


public class DefaultHalApiTypeSupport implements HalApiTypeSupport {

  @Override
  public boolean isHalApiInterface(Class<?> interfaze) {
    return interfaze.isAnnotationPresent(HalApiInterface.class);
  }

  @Override
  public boolean isResourceStateMethod(Method method) {
    return method.isAnnotationPresent(ResourceState.class);
  }

  @Override
  public boolean isRelatedResourceMethod(Method method) {
    return method.isAnnotationPresent(RelatedResource.class);
  }

  @Override
  public boolean isResourceRepresentationMethod(Method method) {
    return method.isAnnotationPresent(ResourceRepresentation.class);
  }

  @Override
  public boolean isResourceLinkMethod(Method method) {
    return method.isAnnotationPresent(ResourceLink.class);
  }

  @Override
  public String getContentType(Class<?> halApiInterface) {
    if (isHalApiInterface(halApiInterface)) {
      return halApiInterface.getAnnotation(HalApiInterface.class).contentType();
    }
    return null;
  }

  @Override
  public String getRelation(Method method) {
    if (isRelatedResourceMethod(method)) {
      return method.getAnnotation(RelatedResource.class).relation();
    }
    return null;
  }


  @SuppressWarnings("unchecked")
  @Override
  public <T> Function<Observable, T> convertFromObservable(Class<T> targetType) {

    if (targetType.isAssignableFrom(Observable.class)) {
      return obs -> (T)obs;
    }
    if (targetType.isAssignableFrom(Single.class)) {
      return obs -> (T)obs.singleOrError();
    }
    if (targetType.isAssignableFrom(Maybe.class)) {
      return obs -> (T)obs.singleElement();
    }
    if (targetType.isAssignableFrom(Publisher.class)) {
      return obs -> (T)obs.toFlowable(BackpressureStrategy.BUFFER);
    }

    if (targetType.isAssignableFrom(Optional.class)) {
      return obs -> (T)obs.singleElement()
          .map(Optional::of)
          .defaultIfEmpty(Optional.empty())
          .blockingGet();
    }
    if (targetType.isAssignableFrom(List.class)) {
      return obs -> (T)obs.toList().blockingGet();
    }

    if (targetType.getTypeParameters().length == 0) {
      return obs -> (T)obs.singleOrError().blockingGet();
    }

    return null;
  }

  @Override
  public Function<? super Object, Observable<?>> convertToObservable(Class<?> sourceType) {

    if (Observable.class.isAssignableFrom(sourceType)) {
      return o -> (Observable)o;
    }
    if (Single.class.isAssignableFrom(sourceType)) {
      return o -> ((Single)o).toObservable();
    }
    if (Maybe.class.isAssignableFrom(sourceType)) {
      return o -> ((Maybe)o).toObservable();
    }
    if (Publisher.class.isAssignableFrom(sourceType)) {
      return o -> Observable.fromPublisher((Publisher<?>)o);
    }

    if (Optional.class.isAssignableFrom(sourceType)) {
      return o -> {
        Optional<?> optional = (Optional)o;
        return optional.isPresent() ? Observable.just(optional.get()) : Observable.empty();
      };
    }
    if (Iterable.class.isAssignableFrom(sourceType)) {
      return o -> Observable.fromIterable((Iterable<?>)o);
    }
    if (sourceType.getTypeParameters().length == 0) {
      return o -> Observable.just(o);
    }

    return null;
  }

  public static HalApiTypeSupport extendWith(HalApiAnnotationSupport annotationSupport, HalApiReturnTypeSupport returnTypeSupport) {

    DefaultHalApiTypeSupport defaultSupport = new DefaultHalApiTypeSupport();
    HalApiTypeSupportAdapter adapter = new HalApiTypeSupportAdapter(annotationSupport, returnTypeSupport);

    return new CompositeHalApiTypeSupport(ImmutableList.of(defaultSupport, adapter));
  }
}
