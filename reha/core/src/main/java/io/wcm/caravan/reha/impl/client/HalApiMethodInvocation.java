/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.reha.impl.client;

import static io.wcm.caravan.reha.impl.reflection.HalApiReflectionUtils.getTemplateVariablesFrom;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import io.wcm.caravan.reha.api.annotations.LinkName;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.annotations.TemplateVariable;
import io.wcm.caravan.reha.api.annotations.TemplateVariables;
import io.wcm.caravan.reha.api.client.HalApiDeveloperException;
import io.wcm.caravan.reha.api.common.HalApiAnnotationSupport;
import io.wcm.caravan.reha.impl.reflection.RxJavaReflectionUtils;


class HalApiMethodInvocation {

  private final Class interfaze;
  private final Method method;
  private final Class<?> emissionType;
  private final HalApiAnnotationSupport annotationSupport;

  private final Map<String, Object> templateVariables;
  private final String linkName;
  private final boolean calledWithOnlyNullParameters;


  HalApiMethodInvocation(Class interfaze, Method method, Object[] args, HalApiAnnotationSupport annotationSupport) {
    this.interfaze = interfaze;
    this.method = method;
    this.emissionType = hasTemplatedReturnType() ? RxJavaReflectionUtils.getObservableEmissionType(method) : method.getReturnType();
    this.annotationSupport = annotationSupport;

    this.templateVariables = new HashMap<>();

    boolean nonNullParameterFound = false;
    String foundLinkName = null;
    for (int i = 0; i < method.getParameterCount(); i++) {
      Parameter parameter = method.getParameters()[i];

      Object parameterValue = args[i];
      nonNullParameterFound = nonNullParameterFound || parameterValue != null;

      TemplateVariable variable = parameter.getAnnotation(TemplateVariable.class);
      LinkName name = parameter.getAnnotation(LinkName.class);
      TemplateVariables variables = parameter.getAnnotation(TemplateVariables.class);

      if (variable != null) {
        templateVariables.put(variable.value(), parameterValue);
      }
      else if (variables != null) {
        templateVariables.putAll(getTemplateVariablesFrom(parameterValue, parameter.getType()));
      }
      else if (name != null) {
        if (foundLinkName != null) {
          throw new HalApiDeveloperException("More than one parameter of " + toString() + " is annotated with @" + LinkName.class.getSimpleName());
        }
        if (parameterValue == null) {
          throw new HalApiDeveloperException(
              "You must provide a non-null value for for the parameter annotated with @" + LinkName.class.getSimpleName() + " when calling " + toString());
        }
        foundLinkName = parameterValue.toString();
      }
      else {
        throw new HalApiDeveloperException("all parameters of " + toString() + " need to be either annotated with @"
            + LinkName.class.getSimpleName() + ", @" + TemplateVariable.class.getSimpleName()
            + " or @" + TemplateVariables.class.getSimpleName());
      }
    }

    this.linkName = foundLinkName;
    this.calledWithOnlyNullParameters = !nonNullParameterFound;
  }


  String getRelation() {
    String relation = annotationSupport.getRelation(method);
    Preconditions.checkNotNull(relation, this + " does not have a @" + RelatedResource.class.getSimpleName() + " annotation");
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

  String getLinkName() {
    return linkName;
  }

  String getDescription() {
    String desc = "emitting {}" + getEmissionType().getSimpleName();
    if (isForMethodAnnotatedWithRelatedResource()) {
      desc += " client proxies";
    }
    else {
      desc += " client state";
    }

    desc += " via " + toString();
    return desc;
  }

  public String getResourceInterfaceName() {
    return interfaze.getSimpleName();
  }

  @Override
  public String toString() {

    return interfaze.getSimpleName() + "#" + method.getName() + "(" + getVariablesString() + ")";
  }

  String getCacheKey() {

    String parameterTypeNames = Stream.of(method.getParameterTypes())
        .map(Class::getName)
        .collect(Collectors.joining(","));

    return interfaze.getName() + "#" + method.getName() + "/" + parameterTypeNames + "?" + getVariablesString();
  }

  private String getVariablesString() {

    return templateVariables.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(","));
  }


}
