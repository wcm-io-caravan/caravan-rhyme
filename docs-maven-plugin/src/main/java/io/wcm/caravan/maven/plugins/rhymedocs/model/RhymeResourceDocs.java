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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Ordering;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.RxJavaReflectionUtils;
import io.wcm.caravan.rhyme.impl.reflection.TemplateVariableDetection;
import io.wcm.caravan.rhyme.impl.reflection.TemplateVariableDetection.Variable;

public class RhymeResourceDocs {

  private static final HalApiTypeSupport TYPE_SUPPORT = new DefaultHalApiTypeSupport();

  private final JavaProjectBuilder builder;

  private final JavaClass apiInterface;

  private final ClassLoader projectClassLoader;

  private final List<RhymeRelatedMethodDocs> relations;

  public RhymeResourceDocs(JavaProjectBuilder builder, JavaClass apiInterface, ClassLoader projectClassLoader) {

    this.builder = builder;

    this.apiInterface = apiInterface;
    this.projectClassLoader = projectClassLoader;

    this.relations = createRelatedMethodDocs(apiInterface);
  }

  private List<RhymeRelatedMethodDocs> createRelatedMethodDocs(JavaClass apiInterface) {

    return apiInterface.getMethods(true).stream()
        .filter(method -> DocumentationUtils.hasAnnotation(method, Related.class))
        .map(RhymeRelatedMethodDocs::new)
        .sorted(Ordering.natural().onResultOf(RhymeRelatedMethodDocs::getRelation))
        .collect(Collectors.toList());
  }

  public String getTitle() {

    return apiInterface.getName();
  }

  public String getDescription() {

    return apiInterface.getComment();
  }

  public String getFullyQualifiedClassName() {

    return apiInterface.getFullyQualifiedName();
  }

  public List<RhymeRelatedMethodDocs> getRelations() {

    return relations;
  }

  class RhymeRelatedMethodDocs {

    private final String relation;
    private final String description;

    private final Class<?> relatedResourceType;
    private final String cardinality;

    private final List<RhymeVariableDocs> variables;

    private RhymeRelatedMethodDocs(JavaMethod javaMethod) {

      Method method = DocumentationUtils.getMethod(apiInterface, javaMethod, projectClassLoader);

      this.relation = method.getAnnotation(Related.class).value();
      this.description = javaMethod.getComment();

      this.relatedResourceType = getRelatedResourceType(method);
      this.cardinality = getCardinality(method);

      this.variables = findVariables(javaMethod, method);
    }

    private Class<?> getRelatedResourceType(Method method) {
      Class<?> type = RxJavaReflectionUtils.getObservableEmissionType(method, TYPE_SUPPORT);

      if (type.getAnnotation(HalApiInterface.class) == null) {
        return null;
      }

      return type;
    }

    private String getCardinality(Method method) {

      Class<?> returnType = method.getReturnType();

      if (TYPE_SUPPORT.isProviderOfOptionalValue(returnType)) {
        return "0..1";
      }

      if (TYPE_SUPPORT.isProviderOfMultiplerValues(returnType)) {
        return "0..n";
      }

      return "1";
    }

    private List<RhymeVariableDocs> findVariables(JavaMethod javaMethod, Method method) {

      List<Variable> variables = TemplateVariableDetection.findVariables(method, Optional.empty());

      return variables.stream()
          .map(var -> new RhymeVariableDocs(var, javaMethod))
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

  public class RhymeVariableDocs {


    private final String name;
    private final String type;
    private final String description;

    public RhymeVariableDocs(String name, String type, String description) {
      this.name = name;
      this.type = type;
      this.description = description;
    }

    public RhymeVariableDocs(Variable var, JavaMethod javaMethod) {

      this.name = var.getName();
      this.type = var.getType().getSimpleName();

      if (var.getDtoClass() == null) {
        this.description = findJavaDocForNamedParameter(javaMethod, name);
      }
      else if (var.getDtoMethod() != null) {
        this.description = findJavaDocForDtoMethod(var.getDtoClass(), var.getDtoMethod());
      }
      else if (var.getDtoField() != null) {
        this.description = findJavaDocForDtoField(var.getDtoClass(), var.getDtoField());
      }
      else {
        this.description = null;
      }
    }

    private String findJavaDocForNamedParameter(JavaMethod javaMethod, String paramName) {

      return javaMethod.getTagsByName("param", true).stream()
          .filter(tag -> !tag.getParameters().isEmpty())
          .filter(tag -> paramName.equals(tag.getParameters().get(0)))
          .flatMap(tag -> tag.getParameters().stream().skip(1))
          .collect(Collectors.joining(" "));
    }

    private String findJavaDocForDtoMethod(Class dtoClass, Method dtoMethod) {

      JavaClass javaClass = builder.getClassByName(dtoClass.getName());
      if (javaClass == null) {
        return null;
      }

      JavaMethod javaMethod = javaClass.getMethods().stream()
          .filter(m -> m.getName().equals(dtoMethod.getName()))
          .findFirst()
          .orElse(null);

      String comment = javaMethod.getComment();
      if (StringUtils.isNotBlank(comment)) {
        return comment;
      }

      DocletTag returnTag = javaMethod.getTagByName("return");
      if (returnTag != null) {
        return returnTag.getValue();
      }

      return "";
    }

    private String findJavaDocForDtoField(Class dtoClass, Field dtoField) {

      JavaClass javaClass = builder.getClassByName(dtoClass.getName());
      if (javaClass == null) {
        return null;
      }

      JavaField javaField = javaClass.getFieldByName(dtoField.getName());

      return StringUtils.trimToEmpty(javaField.getComment());
    }

    public String getName() {
      return this.name;
    }

    public String getType() {
      return this.type;
    }

    public String getDescription() {
      return this.description;
    }
  }
}
