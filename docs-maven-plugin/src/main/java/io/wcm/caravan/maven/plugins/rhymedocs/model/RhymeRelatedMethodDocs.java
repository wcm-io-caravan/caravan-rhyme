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
package io.wcm.caravan.maven.plugins.rhymedocs.model;

import static io.wcm.caravan.maven.plugins.rhymedocs.model.DocumentationUtils.getMethodsWithAnnotation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Ordering;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.rhyme.impl.reflection.TemplateVariableDetection;
import io.wcm.caravan.rhyme.impl.reflection.TemplateVariableDetection.Variable;

/**
 * provides documentation a method annotated with {@link Related} that corresponds to a ling relation
 */
public final class RhymeRelatedMethodDocs {

  private final JavaProjectBuilder builder;

  private final String relation;
  private final String description;

  private final Class<?> relatedResourceType;
  private final String cardinality;

  private final List<RhymeVariableDocs> variables;

  private RhymeRelatedMethodDocs(JavaClass apiInterface, JavaMethod javaMethod, JavaProjectBuilder builder, ClassLoader projectClassLoader) {

    this.builder = builder;

    Method method = DocumentationUtils.getMethod(apiInterface, javaMethod, projectClassLoader);

    this.relation = method.getAnnotation(Related.class).value();
    this.description = javaMethod.getComment();

    this.relatedResourceType = getRelatedResourceType(method);
    this.cardinality = getCardinality(method);

    this.variables = findVariables(javaMethod, method);
  }

  static List<RhymeRelatedMethodDocs> create(JavaClass apiInterface, JavaProjectBuilder builder, ClassLoader projectClassLoader) {

    return getMethodsWithAnnotation(apiInterface, Related.class)
        .map(method -> new RhymeRelatedMethodDocs(apiInterface, method, builder, projectClassLoader))
        .sorted(Ordering.natural().onResultOf(RhymeRelatedMethodDocs::getRelation))
        .collect(Collectors.toList());
  }

  private Class<?> getRelatedResourceType(Method method) {
    Class<?> type = RxJavaReflectionUtils.getObservableEmissionType(method, RhymeResourceDocs.TYPE_SUPPORT);

    if (type.getAnnotation(HalApiInterface.class) == null) {
      return null;
    }

    return type;
  }

  private String getCardinality(Method method) {

    Class<?> returnType = method.getReturnType();

    if (RhymeResourceDocs.TYPE_SUPPORT.isProviderOfOptionalValue(returnType)) {
      return "0..1";
    }

    if (RhymeResourceDocs.TYPE_SUPPORT.isProviderOfMultiplerValues(returnType)) {
      return "0..n";
    }

    return "1";
  }

  private List<RhymeVariableDocs> findVariables(JavaMethod javaMethod, Method method) {

    List<Variable> variables = TemplateVariableDetection.findVariables(method, Optional.empty());

    return variables.stream()
        .map(var -> new RhymeVariableDocs(builder, var, javaMethod))
        .collect(Collectors.toList());
  }

  public String getRelation() {

    return relation;
  }

  public String getCardinality() {

    return cardinality;
  }

  public String getRelatedResourceTitle() {

    if (relatedResourceType == null) {
      return null;
    }

    return relatedResourceType.getSimpleName();
  }

  public String getRelatedResourceHref() {

    if (relatedResourceType == null) {
      return null;
    }

    return relatedResourceType.getName() + ".html";
  }

  public String getDescription() {

    return description;
  }

  public List<RhymeVariableDocs> getVariables() {

    return variables;
  }
}
