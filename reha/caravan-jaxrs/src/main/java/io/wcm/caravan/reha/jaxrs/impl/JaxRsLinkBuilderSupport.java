/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
package io.wcm.caravan.reha.jaxrs.impl;

import static io.wcm.caravan.reha.jaxrs.impl.JaxRsReflectionUtils.findFieldsDefinedInClass;
import static io.wcm.caravan.reha.jaxrs.impl.JaxRsReflectionUtils.findFieldsThatContainOtherParamsIn;
import static io.wcm.caravan.reha.jaxrs.impl.JaxRsReflectionUtils.getField;
import static io.wcm.caravan.reha.jaxrs.impl.JaxRsReflectionUtils.getFieldValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.api.server.LinkBuilderSupport;

/**
 * Implements the JAX-RS specific logic to automatically build links to JAX-RS resource implementations by scanning the
 * classes for {@link Path}, {@link QueryParam}, {@link BeanParam} and {@link PathParam} annotations
 */
public final class JaxRsLinkBuilderSupport implements LinkBuilderSupport {

  // finding the fields that correspond to parameters through reflection is quite expensive (compared to just reading the value).
  // We want to avoid doing that for each resource *instance*, so we cache a finder object for each resource implementation class
  private final Cache<Class, PathParamFinder> pathParamFinderCache = CacheBuilder.newBuilder().maximumSize(100).build();
  private final Cache<Class, QueryParamFinder> queryParamFinderCache = CacheBuilder.newBuilder().maximumSize(100).build();

  @Override
  public String getResourcePathTemplate(LinkableResource targetResource) {

    Path pathAnnotation = targetResource.getClass().getAnnotation(Path.class);
    Preconditions.checkNotNull(pathAnnotation,
        "A @Path annotation must be present on the resource implementation class (" + targetResource.getClass().getName() + ")");

    return pathAnnotation.value();
  }

  @Override
  public Map<String, Object> getPathParameters(LinkableResource targetResource) {

    Class resourceImplClass = targetResource.getClass();
    try {
      PathParamFinder finder = pathParamFinderCache.get(resourceImplClass, () -> new PathParamFinder(resourceImplClass));
      return finder.getParameterMap(targetResource);
    }
    catch (ExecutionException | UncheckedExecutionException ex) {
      throw new RuntimeException("Failed to access @PathParam fields in " + resourceImplClass, ex);
    }
  }

  @Override
  public Map<String, Object> getQueryParameters(LinkableResource targetResource) {

    Class resourceImplClass = targetResource.getClass();
    try {
      QueryParamFinder finder = queryParamFinderCache.get(resourceImplClass, () -> new QueryParamFinder(resourceImplClass));
      return finder.getParameterMap(targetResource);
    }
    catch (ExecutionException | UncheckedExecutionException ex) {
      throw new RuntimeException("Failed to access @QueryParam fields in " + resourceImplClass, ex);
    }
  }


  /**
   * Contains the logic shared between {@link PathParamFinder} and {@link QueryParamFinder} classes
   * @param <T> the annotation to look for
   */
  private abstract static class AnnotatedParameterFinder<T extends Annotation> {

    private final Class<T> annotationClass;

    private final Collection<ParamValueProvider> paramValueProviders;

    AnnotatedParameterFinder(Class resourceImplClass, Class<T> annotationClass) {

      this.annotationClass = annotationClass;

      // there are three ways to extract parameter values from a class, each of which is implemented in its own factory
      Stream<ParamValueProviderFactory> valueProviderFactories = Stream
          .of(new AnnotatedFieldValueProviderFactory(), new AnnotatedConstructorValueProviderFactory(), new AnnotatedBeanParamValueProviderFactory());

      // create one value provider for each parameter present in the given resource class
      this.paramValueProviders = valueProviderFactories
          .flatMap(f -> f.getValueProviders(this, resourceImplClass))
          .collect(Collectors.toList());
    }

    /**
     * @param resourceImpl the server-side instance of
     * @return a map of all query and path parameters names and values (which can be null)
     */
    Map<String, Object> getParameterMap(Object resourceImpl) {

      Map<String, Object> parameters = new HashMap<>();

      paramValueProviders.forEach(provider -> parameters.put(provider.getParamName(), provider.getParamValue(resourceImpl)));

      return parameters;
    }

    private String getParameterNameFromAnnotation(AnnotatedElement p) {
      T paramAnnotation = p.getAnnotation(annotationClass);
      if (paramAnnotation == null) {
        return null;
      }
      return getParameterNameFromAnnotation(paramAnnotation);
    }

    protected abstract AnnotatedParameterFinder<T> createFinderForBeanParamOfType(Class<?> type);

    protected abstract String getParameterNameFromAnnotation(T paramAnnotation);

  }

  /**
   * Finds fields annotated with {@link QueryParam} in the given resource implementation class
   */
  static class QueryParamFinder extends AnnotatedParameterFinder<QueryParam> {

    QueryParamFinder(Class resourceImplClass) {
      super(resourceImplClass, QueryParam.class);
    }

    @Override
    protected AnnotatedParameterFinder<QueryParam> createFinderForBeanParamOfType(Class<?> type) {
      return new QueryParamFinder(type);
    }

    @Override
    protected String getParameterNameFromAnnotation(QueryParam paramAnnotation) {
      return paramAnnotation.value();
    }

  }

  /**
   * Finds fields annotated with {@link PathParam} in the given resource implementation class
   */
  static class PathParamFinder extends AnnotatedParameterFinder<PathParam> {

    PathParamFinder(Class resourceImplClass) {
      super(resourceImplClass, PathParam.class);
    }

    @Override
    protected AnnotatedParameterFinder<PathParam> createFinderForBeanParamOfType(Class<?> type) {
      return new PathParamFinder(type);
    }

    @Override
    protected String getParameterNameFromAnnotation(PathParam paramAnnotation) {
      return paramAnnotation.value();
    }
  }

  /**
   * extract a single parameter value (and its name) from a given resource instance
   */
  private interface ParamValueProvider {

    String getParamName();

    Object getParamValue(Object resourceInstance);
  }

  /**
   * Common interface for the factories that implement the logic for looking up fields that correspond to parameters
   */
  private interface ParamValueProviderFactory {

    /**
     * @param finder the {@link PathParamFinder} or {@link QueryParamFinder} that uses this factory
     * @param resourceImplClass the JAX-RS resource class
     * @return a stream with providers to extract the value of all parameters identified by this factory
     */
    Stream<ParamValueProvider> getValueProviders(AnnotatedParameterFinder<?> finder, Class resourceImplClass);
  }

  /**
   * Implements the lookup of fields that are directly annotated with {@link PathParam} or {@link QueryParam}
   * annotations
   */
  static class AnnotatedFieldValueProviderFactory implements ParamValueProviderFactory {

    @Override
    public Stream<ParamValueProvider> getValueProviders(AnnotatedParameterFinder<?> finder, Class resourceImplClass) {

      // iterate over all fields
      return findFieldsDefinedInClass(resourceImplClass).stream()
          // let the annotation-specific finder try to extract a parameter name from the annotation
          .map(field -> Pair.of(finder.getParameterNameFromAnnotation(field), field))
          // ignore all fields that don't have the desired annotation
          .filter(pair -> pair.getKey() != null)
          // then create a ParamValueProvider that just reads the value of the annotated field
          .map(pair -> new ParamValueProvider() {

            @Override
            public String getParamName() {
              return pair.getKey();
            }

            @Override
            public Object getParamValue(Object resourceInstance) {
              return getFieldValue(pair.getValue(), resourceInstance);
            }

          });
    }
  }

  /**
   * Implement the lookup of fields that have the same name as an annotated constructor parameter
   */
  static class AnnotatedConstructorValueProviderFactory implements ParamValueProviderFactory {

    @Override
    public Stream<ParamValueProvider> getValueProviders(AnnotatedParameterFinder<?> finder, Class resourceImplClass) {

      // iterate over all parameters used in any constructor of the class
      return Stream.of(resourceImplClass.getConstructors())
          .flatMap(constructor -> Stream.of(constructor.getParameters()))
          // let the annotation-specific finder try to extract a parameter name from the annotation
          .map(constructorParam -> finder.getParameterNameFromAnnotation(constructorParam))
          // ignore all parameters that don't have the desired annotation
          .filter(Objects::nonNull)
          // ignore duplicate values (that can occur if there are multiple constructors)
          .distinct()
          // then create a ParamValueProvider that just reads the value of the field that has the same name as the constructor parameter
          .map(name -> new ParamValueProvider() {

            private Field field = getField(resourceImplClass, name);

            @Override
            public String getParamName() {
              return name;
            }

            @Override
            public Object getParamValue(Object resourceInstance) {
              return getFieldValue(field, resourceInstance);
            }

          });
    }
  }

  /**
   * Implement the logic of finding (and accessing) DTO fields, i.e. fields that are not directly annotated with
   * {@link PathParam} or {@link QueryParam} but are composite object that contain such fields
   */
  static class AnnotatedBeanParamValueProviderFactory implements ParamValueProviderFactory {

    @Override
    public Stream<ParamValueProvider> getValueProviders(AnnotatedParameterFinder<?> finder, Class resourceImplClass) {

      // find all fields with "DTO" types (that compose multiple annotated parameter properties)
      return findFieldsThatContainOtherParamsIn(resourceImplClass)
          .flatMap(dtoField -> {

            // create a new PathParamFinder or QueryParamFinder *for the DTO type*
            AnnotatedParameterFinder<?> finderForDtoType = finder.createFinderForBeanParamOfType(dtoField.getType());
            // extract the value providers that are able to extract the parameter values *from the DTO instance*
            Collection<ParamValueProvider> valueProviders = finderForDtoType.paramValueProviders;

            // another level of indirection is required, because we need to be able to extract the value from the *resource* instance...
            return valueProviders.stream()
                .map(provider -> new ParamValueProvider() {

                  @Override
                  public String getParamName() {
                    return provider.getParamName();
                  }

                  @Override
                  public Object getParamValue(Object resourceInstance) {
                    // first get the DTO instance from the resource
                    Object dtoValue = getFieldValue(dtoField, resourceInstance);
                    // then extract the parameter value from the DTO
                    return getFieldValue(provider.getParamName(), dtoValue);
                  }

                });
          });
    }
  }
}
