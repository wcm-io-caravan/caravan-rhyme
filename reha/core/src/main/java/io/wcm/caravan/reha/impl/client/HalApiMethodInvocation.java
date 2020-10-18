/*-
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 - 2020 wcm.io Caravan
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

package io.wcm.caravan.reha.impl.client;

import static io.wcm.caravan.reha.impl.reflection.HalApiReflectionUtils.getTemplateVariablesFrom;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import io.wcm.caravan.reha.api.annotations.Related;
import io.wcm.caravan.reha.api.annotations.TemplateVariable;
import io.wcm.caravan.reha.api.annotations.TemplateVariables;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.reha.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.reha.impl.reflection.RxJavaReflectionUtils;


class HalApiMethodInvocation {

  private final Class interfaze;
  private final Method method;
  private final Class<?> emissionType;
  private final HalApiAnnotationSupport annotationSupport;

  private final Map<String, Object> templateVariables;
  private final boolean calledWithOnlyNullParameters;


  HalApiMethodInvocation(Class interfaze, Method method, Object[] args, HalApiAnnotationSupport annotationSupport) {
    this.interfaze = interfaze;
    this.method = method;
    this.emissionType = hasTemplatedReturnType() ? RxJavaReflectionUtils.getObservableEmissionType(method) : method.getReturnType();
    this.annotationSupport = annotationSupport;

    this.templateVariables = new LinkedHashMap<>();

    boolean nonNullParameterFound = false;

    for (int i = 0; i < method.getParameterCount(); i++) {
      Parameter parameter = method.getParameters()[i];

      Object parameterValue = args[i];
      nonNullParameterFound = nonNullParameterFound || parameterValue != null;

      TemplateVariable variable = parameter.getAnnotation(TemplateVariable.class);
      TemplateVariables variables = parameter.getAnnotation(TemplateVariables.class);

      if (variable != null) {
        templateVariables.put(variable.value(), parameterValue);
      }
      else if (variables != null) {
        templateVariables.putAll(getTemplateVariablesFrom(parameterValue, parameter.getType()));
      }
      else {
        throw new HalApiDeveloperException("all parameters of " + toString() + " need to be either annotated with"
            + " @" + TemplateVariable.class.getSimpleName()
            + " or @" + TemplateVariables.class.getSimpleName());
      }
    }

    this.calledWithOnlyNullParameters = !nonNullParameterFound;
  }


  String getRelation() {
    String relation = annotationSupport.getRelation(method);
    Preconditions.checkNotNull(relation, this + " does not have a @" + Related.class.getSimpleName() + " annotation");
    return relation;
  }

  boolean isForMethodAnnotatedWithRelatedResource() {
    return annotationSupport.isRelatedResourceMethod(method);
  }

  boolean isForMethodAnnotatedWithResourceLink() {
    return annotationSupport.isResourceLinkMethod(method);
  }

  boolean isForMethodAnnotatedWithResourceState() {
    return annotationSupport.isResourceStateMethod(method);
  }

  boolean isForMethodAnnotatedWithResourceRepresentation() {
    return annotationSupport.isResourceRepresentationMethod(method);
  }

  boolean hasTemplatedReturnType() {
    return method.getGenericReturnType() instanceof ParameterizedType;
  }

  Class<?> getReturnType() {
    return method.getReturnType();
  }

  Class<?> getEmissionType() {
    return emissionType;
  }

  boolean isCalledWithOnlyNullParameters() {
    return calledWithOnlyNullParameters;
  }

  Map<String, Object> getTemplateVariables() {
    return templateVariables;
  }

  String getDescription() {

    StringBuilder desc = new StringBuilder("emitting {}")
        .append(getEmissionType().getSimpleName());

    if (isForMethodAnnotatedWithRelatedResource()) {
      desc.append(" client proxies");
    }
    else {
      desc.append(" client state");
    }

    desc.append(" via ").append(toString());

    return desc.toString();
  }

  public String getResourceInterfaceName() {
    return interfaze.getSimpleName();
  }

  @Override
  public String toString() {

    return interfaze.getSimpleName() + "#" + method.getName() + "(" + getVariableNamesString() + ")";
  }

  String getCacheKey() {

    String parameterTypeNames = Stream.of(method.getParameterTypes())
        .map(Class::getName)
        .collect(Collectors.joining(","));

    return interfaze.getName() + "#" + method.getName() + "/" + parameterTypeNames + "?" + getVariablesString();
  }

  private String getVariableNamesString() {

    return templateVariables.keySet().stream()
        .collect(Collectors.joining(","));
  }

  private String getVariablesString() {

    return templateVariables.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(","));
  }


}
