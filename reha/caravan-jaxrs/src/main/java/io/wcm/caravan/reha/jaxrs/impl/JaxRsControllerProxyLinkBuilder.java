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
package io.wcm.caravan.reha.jaxrs.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncheckedExecutionException;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.client.HalApiDeveloperException;
import io.wcm.caravan.reha.jaxrs.api.JaxRsLinkBuilder;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

public class JaxRsControllerProxyLinkBuilder<T> implements InvocationHandler, JaxRsLinkBuilder<T> {

  private static final String CAPTURED_URI_FIELD_NAME = "__capturedUri";

  private final String baseUrl;

  private final Class<T> controllerClass;
  private final Class<? extends T> proxyClass;

  private final List<TemplateParameter> additionalQueryParameters = new LinkedList<>();

  private final Cache<Method, MethodDetails> cache = CacheBuilder.newBuilder().build();

  public JaxRsControllerProxyLinkBuilder(String baseUrl, Class<T> controllerClass) {

    this.baseUrl = baseUrl;
    this.controllerClass = controllerClass;

    this.proxyClass = createProxyClass(controllerClass);
  }

  private Class<? extends T> createProxyClass(Class<T> superClass) {

    return new ByteBuddy()
        .subclass(superClass)
        .method(ElementMatchers.any())
        .intercept(InvocationHandlerAdapter.of(this))
        .defineField(CAPTURED_URI_FIELD_NAME, String.class, java.lang.reflect.Modifier.PUBLIC)
        .make()
        .load(superClass.getClassLoader())
        .getLoaded();
  }

  @Override
  public JaxRsLinkBuilder<T> withAdditionalQueryParameters(Map<String, Object> parameters) {

    parameters.forEach((name, value) -> {

      TemplateParameter param = new TemplateParameter();
      param.name = name;
      param.iterable = value instanceof Iterable;
      param.query = true;
      param.required = true;
      param.valueProvider = args -> value;

      additionalQueryParameters.add(param);
    });

    return this;
  }

  @Override
  public Link buildLinkTo(Consumer<T> consumer) {

    try {
      T instance = proxyClass.newInstance();

      consumer.accept(instance);

      String url = (String)FieldUtils.readField(instance, CAPTURED_URI_FIELD_NAME);

      return new Link(url);
    }
    catch (InstantiationException | IllegalAccessException ex) {
      throw new RuntimeException("Failed to instantiate proxy for " + controllerClass.getName(), ex);
    }
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    MethodDetails details = getCachedMethodDetails(method);

    String uriTemplate = details.getUriTemplate(args);

    FieldUtils.writeField(proxy, CAPTURED_URI_FIELD_NAME, uriTemplate, false);

    return null;
  }

  private JaxRsControllerProxyLinkBuilder<T>.MethodDetails getCachedMethodDetails(Method method) throws ExecutionException {

    try {
      return cache.get(method, () -> new MethodDetails(controllerClass, method));
    }
    catch (UncheckedExecutionException ex) {
      if (ex.getCause() instanceof RuntimeException) {
        throw (RuntimeException)ex.getCause();
      }
      throw ex;
    }
  }

  private static class TemplateParameter {

    private String name;
    private boolean iterable;
    private boolean query;
    private boolean required;

    private Function<Object[], Object> valueProvider;

    String getVarName() {
      String varName = name;
      if (iterable) {
        varName += "*";
      }
      return varName;
    }

    ImmutablePair<String, Object> resolve(Object[] args) {
      Object value = valueProvider.apply(args);
      if (value == null) {
        return null;
      }
      return new ImmutablePair<>(name, value);
    }
  }

  private class MethodDetails {

    private final String methodName;
    private final Method method;

    private final List<TemplateParameter> parameters = new ArrayList<>();;

    private final String templatePath;

    MethodDetails(Class<?> controllerClass, Method method) {

      this.methodName = controllerClass.getName() + "#" + method.getName();
      this.method = method;

      this.templatePath = baseUrl + getPathFromAnnotation();

      this.parameters.addAll(getQueryParameters());
      this.parameters.addAll(getPathParameters());
      this.parameters.addAll(getBeanParameters());

      this.parameters.addAll(additionalQueryParameters);
    }

    private String getPathFromAnnotation() {

      Path pathAnnotation = method.getAnnotation(Path.class);
      if (pathAnnotation == null) {
        throw new HalApiDeveloperException("The method " + methodName + " must have a @" + Path.class.getSimpleName() + " annotation");
      }

      return pathAnnotation.value();
    }

    private List<TemplateParameter> getQueryParameters() {

      return new QueryParamFinder().findParameters();
    }

    private List<TemplateParameter> getPathParameters() {

      return new PathParamFinder().findParameters();
    }

    private List<TemplateParameter> getBeanParameters() {

      return new BeanParamFinder().findParameters();
    }

    private Map<String, Object> getParameterMap(Object[] args) {

      return parameters.stream()
          .map(tp -> tp.resolve(args))
          .filter(Objects::nonNull)
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    String getUriTemplate(Object[] args) {

      Map<String, Object> parameterMap = getParameterMap(args);

      String queries = createQueryParamsTemplate(parameterMap);

      UriTemplate template = UriTemplate.fromTemplate(templatePath + queries);
      template.set(parameterMap);
      return template.expandPartial();
    }

    private String createQueryParamsTemplate(Map<String, Object> parameterMap) {
      String queries = "";

      String[] sortedVarNames = getQueryParameterVarNames(tp -> parameterMap.containsKey(tp.name));
      for (String varName : sortedVarNames) {
        queries += queries.isEmpty() ? "{?" : ",";
        queries += varName;
      }
      if (!queries.isEmpty()) {
        queries += "}";
      }

      return queries;
    }

    private String[] getQueryParameterVarNames(Predicate<TemplateParameter> valueAvailableCheck) {

      Comparator<TemplateParameter> comparator = (p1, p2) -> {
        boolean p1Resolved = valueAvailableCheck.test(p1);
        boolean p2Resolved = valueAvailableCheck.test(p2);
        if (p1Resolved && !p2Resolved) {
          return -1;
        }
        if (p2Resolved && !p1Resolved) {
          return 1;
        }
        return 0;
      };

      boolean allRequiredParametersResolved = parameters.stream()
          .filter(tp -> tp.required)
          .allMatch(tp -> valueAvailableCheck.test(tp));

      String[] varSpecs = parameters.stream()
          .filter(tp -> tp.query)
          .filter(tp -> !allRequiredParametersResolved || tp.required || valueAvailableCheck.test(tp))
          .sorted(comparator)
          .map(TemplateParameter::getVarName)
          .toArray(String[]::new);

      return varSpecs;
    }

    abstract class AnnotatedParamFinder<AnnotationType extends Annotation> {

      protected final Class<AnnotationType> annotationClass;

      AnnotatedParamFinder(Class<AnnotationType> annotationClass) {
        this.annotationClass = annotationClass;
      }

      List<TemplateParameter> findParameters() {

        List<TemplateParameter> specs = new LinkedList<>();

        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
          if (params[i].isAnnotationPresent(annotationClass)) {

            specs.addAll(createTemplateParameters(params[i], i));
          }
        }
        return specs;
      }

      protected boolean isOfIterableType(Parameter p) {
        return Iterable.class.isAssignableFrom(p.getType());
      }

      protected boolean isOfIterableType(Field f) {
        return Iterable.class.isAssignableFrom(f.getType());
      }

      protected abstract Collection<TemplateParameter> createTemplateParameters(Parameter param, int index);
    }

    class QueryParamFinder extends AnnotatedParamFinder<QueryParam> {

      QueryParamFinder() {
        super(QueryParam.class);
      }

      @Override
      protected Collection<TemplateParameter> createTemplateParameters(Parameter p, int index) {

        TemplateParameter parameter = new TemplateParameter();

        parameter.name = p.getAnnotation(annotationClass).value();
        parameter.iterable = isOfIterableType(p);
        parameter.query = true;
        parameter.required = !p.isAnnotationPresent(DefaultValue.class);
        parameter.valueProvider = (args -> args[index]);

        return ImmutableList.of(parameter);
      }
    }

    class PathParamFinder extends AnnotatedParamFinder<PathParam> {

      PathParamFinder() {
        super(PathParam.class);
      }

      @Override
      protected Collection<TemplateParameter> createTemplateParameters(Parameter p, int index) {

        TemplateParameter parameter = new TemplateParameter();

        parameter.name = p.getAnnotation(annotationClass).value();
        parameter.iterable = isOfIterableType(p);
        parameter.query = false;
        parameter.required = true;
        parameter.valueProvider = (args -> args[index]);

        return ImmutableList.of(parameter);
      }
    }

    class BeanParamFinder extends AnnotatedParamFinder<BeanParam> {

      BeanParamFinder() {
        super(BeanParam.class);
      }

      @Override
      protected Collection<TemplateParameter> createTemplateParameters(Parameter p, int index) {

        List<TemplateParameter> paramsInBean = new LinkedList<>();

        Class<?> beanType = p.getType();
        Field[] fields = FieldUtils.getAllFields(beanType);

        Stream.of(fields)
            .filter(field -> field.isAnnotationPresent(QueryParam.class))
            .map(field -> handleQueryParamField(index, field))
            .forEach(paramsInBean::add);

        Stream.of(fields)
            .filter(field -> field.isAnnotationPresent(PathParam.class))
            .map(field -> handlePathParamField(index, field))
            .forEach(paramsInBean::add);

        return paramsInBean;
      }

      private TemplateParameter handleQueryParamField(int index, Field field) {
        QueryParam qp = field.getAnnotation(QueryParam.class);

        TemplateParameter parameter = new TemplateParameter();
        parameter.name = qp.value();
        parameter.iterable = isOfIterableType(field);
        parameter.query = true;
        parameter.required = !field.isAnnotationPresent(DefaultValue.class);
        parameter.valueProvider = (args -> JaxRsReflectionUtils.getFieldValue(field, args[index]));

        return parameter;
      }

      private TemplateParameter handlePathParamField(int index, Field field) {
        PathParam pp = field.getAnnotation(PathParam.class);

        TemplateParameter parameter = new TemplateParameter();
        parameter.name = pp.value();
        parameter.iterable = isOfIterableType(field);
        parameter.query = false;
        parameter.required = true;
        parameter.valueProvider = (args -> JaxRsReflectionUtils.getFieldValue(field, args[index]));

        return parameter;
      }
    }
  }

}
