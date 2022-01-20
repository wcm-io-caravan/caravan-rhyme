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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import com.google.common.collect.ImmutableList;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceLink;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.annotations.ResourceRepresentation;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;

/**
 * The default implementation of {@link HalApiTypeSupport} that allows the following types to be used as return types
 * for methods annotated with {@link Related} and {@link ResourceState}:
 * {@link Observable}, {@link Single}, {@link Maybe}, {@link Publisher}, {@link Optional}, {@link List}
 */
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
  public boolean isResourcePropertyMethod(Method method) {
    return method.isAnnotationPresent(ResourceProperty.class);
  }

  @Override
  public String getPropertyName(Method method) {
    if (isResourcePropertyMethod(method)) {
      return method.getAnnotation(ResourceProperty.class).value();
    }
    return null;
  }

  @Override
  public boolean isRelatedResourceMethod(Method method) {
    return method.isAnnotationPresent(Related.class);
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
      return method.getAnnotation(Related.class).value();
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
    if (targetType.isAssignableFrom(Stream.class)) {
      return obs -> (T)((List<?>)obs.toList().blockingGet()).stream();
    }

    if (targetType.getTypeParameters().length == 0) {
      return obs -> (T)obs.singleOrError().blockingGet();
    }

    return null;
  }

  @Override
  public Function<Object, Observable<Object>> convertToObservable(Class<?> sourceType) {

    if (Observable.class.isAssignableFrom(sourceType)) {
      return Observable.class::cast;
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
    if (List.class.isAssignableFrom(sourceType)) {
      return o -> Observable.fromIterable((List<?>)o);
    }
    if (Stream.class.isAssignableFrom(sourceType)) {
      return o -> {
        @SuppressWarnings("unchecked")
        Stream<Object> stream = (Stream<Object>)o;
        return Observable.fromStream(stream);
      };
    }
    if (sourceType.getTypeParameters().length == 0) {
      return Observable::just;
    }

    return null;
  }

  @Override
  public boolean isProviderOfMultiplerValues(Class<?> returnType) {

    return List.class.isAssignableFrom(returnType)
        || Stream.class.isAssignableFrom(returnType)
        || Observable.class.isAssignableFrom(returnType)
        || Publisher.class.isAssignableFrom(returnType);
  }

  @Override
  public boolean isProviderOfOptionalValue(Class<?> returnType) {

    return Optional.class.isAssignableFrom(returnType)
        || Maybe.class.isAssignableFrom(returnType);
  }

  /**
   * @param annotationSupport additional support for different annotations
   * @param returnTypeSupport additional support for different return types
   * @return an {@link HalApiTypeSupport} instance that combines the logic of the {@link DefaultHalApiTypeSupport} and
   *         the given additional SPI implementations
   */
  public static HalApiTypeSupport extendWith(HalApiAnnotationSupport annotationSupport, HalApiReturnTypeSupport returnTypeSupport) {

    DefaultHalApiTypeSupport defaultSupport = new DefaultHalApiTypeSupport();
    HalApiTypeSupportAdapter adapter = new HalApiTypeSupportAdapter(annotationSupport, returnTypeSupport);

    return new CompositeHalApiTypeSupport(ImmutableList.of(defaultSupport, adapter));
  }


}
