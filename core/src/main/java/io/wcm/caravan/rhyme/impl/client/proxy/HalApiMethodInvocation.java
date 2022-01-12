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

package io.wcm.caravan.rhyme.impl.client.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.rhyme.impl.reflection.TemplateVariableDetection;


class HalApiMethodInvocation {

  private final Class interfaze;
  private final Method method;
  private final Class<?> emissionType;
  private final HalApiTypeSupport typeSupport;

  private final Map<String, Object> templateVariables;
  private final boolean calledWithOnlyNullParameters;


  HalApiMethodInvocation(RequestMetricsCollector metrics, Class interfaze, Method method, Object[] args, HalApiTypeSupport typeSupport) {

    try (RequestMetricsStopwatch sw = metrics.startStopwatch(HalApiClient.class, () -> "creating HalApiMethodInvocation instances")) {
      this.interfaze = interfaze;
      this.method = method;
      this.emissionType = hasTemplatedReturnType() ? RxJavaReflectionUtils.getObservableEmissionType(method, typeSupport) : method.getReturnType();
      this.typeSupport = typeSupport;

      this.templateVariables = TemplateVariableDetection.getVariablesNameValueMap(method, Optional.ofNullable(args));

      this.calledWithOnlyNullParameters = args != null && Stream.of(args).allMatch(Objects::isNull);
    }
  }


  String getRelation() {
    String relation = typeSupport.getRelation(method);
    Preconditions.checkNotNull(relation, this + " does not have a @" + Related.class.getSimpleName() + " annotation");
    return relation;
  }

  boolean isForMethodAnnotatedWithRelatedResource() {
    return typeSupport.isRelatedResourceMethod(method);
  }

  boolean isForMethodAnnotatedWithResourceLink() {
    return typeSupport.isResourceLinkMethod(method);
  }

  boolean isForMethodAnnotatedWithResourceState() {
    return typeSupport.isResourceStateMethod(method);
  }

  boolean isForMethodAnnotatedWithResourceProperties() {
    return typeSupport.isResourcePropertyMethod(method);
  }

  boolean isForMethodAnnotatedWithResourceRepresentation() {
    return typeSupport.isResourceRepresentationMethod(method);
  }

  boolean hasTemplatedReturnType() {
    return method.getGenericReturnType() instanceof ParameterizedType;
  }

  Method getMethod() {
    return method;
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

    StringBuilder desc = new StringBuilder("emitting ")
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
