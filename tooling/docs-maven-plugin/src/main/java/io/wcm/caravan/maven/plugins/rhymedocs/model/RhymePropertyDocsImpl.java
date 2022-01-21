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

import static org.apache.commons.lang3.ClassUtils.isPrimitiveOrWrapper;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.TreeNode;
import com.google.common.collect.Ordering;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;

import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.impl.reflection.RxJavaReflectionUtils;

public abstract class RhymePropertyDocsImpl implements RhymePropertyDocs {

  protected final JavaProjectBuilder builder;
  protected final Class stateType;
  protected final String basePointer;

  protected RhymePropertyDocsImpl(JavaProjectBuilder builder, Class stateType, String basePointer) {
    this.builder = builder;
    this.stateType = stateType;
    this.basePointer = basePointer;
  }

  /* (non-Javadoc)
   * @see io.wcm.caravan.maven.plugins.rhymedocs.model.RhymePropertyDocs#getJsonPointer()
   */
  @Override
  public String getJsonPointer() {
    return this.basePointer + "/" + getPropertyName();
  }

  /* (non-Javadoc)
   * @see io.wcm.caravan.maven.plugins.rhymedocs.model.RhymePropertyDocs#getType()
   */
  @Override
  public String getType() {

    String typeName = this.getPropertyClass().getSimpleName();

    String typeParameters = getTypeArguments(getPropertyGenericType())
        .map(Class::getSimpleName)
        .collect(Collectors.joining(", "));

    if (StringUtils.isNotBlank(typeParameters)) {
      typeName += "<" + typeParameters + ">";
    }

    return typeName;
  }

  public Class<?> getFirstTypeParameter() {

    return getTypeArguments(getPropertyGenericType())
        .findFirst()
        .orElse(null);
  }

  /* (non-Javadoc)
   * @see io.wcm.caravan.maven.plugins.rhymedocs.model.RhymePropertyDocs#getDescription()
   */
  @Override
  public abstract String getDescription();

  protected abstract Type getPropertyGenericType();

  protected abstract Class<?> getPropertyClass();

  protected abstract String getPropertyName();


  public static List<RhymePropertyDocs> create(JavaClass apiInterface, JavaProjectBuilder builder, ClassLoader projectClassLoader) {

    Stream<RhymePropertyDocs> resourceStateDocs = crreateDocsFromResourceState(apiInterface, builder, projectClassLoader);

    Stream<RhymePropertyDocs> resourcePropertyDocs = crreateDocsFromResourceProperty(apiInterface, builder, projectClassLoader);

    return Stream.concat(resourceStateDocs, resourcePropertyDocs)
        .collect(Collectors.toList());
  }

  private static Stream<RhymePropertyDocs> crreateDocsFromResourceProperty(JavaClass apiInterface, JavaProjectBuilder builder, ClassLoader projectClassLoader) {

    Stream<JavaMethod> javaMethods = DocumentationUtils.getMethodsWithAnnotation(apiInterface, ResourceProperty.class);

    return javaMethods.map(javaMethod -> {

      Method method = DocumentationUtils.getMethod(apiInterface, javaMethod, projectClassLoader);
      Class<?> stateType = RxJavaReflectionUtils.getObservableEmissionType(method, RhymeResourceDocs.TYPE_SUPPORT);

      String description = DocumentationUtils.findJavaDocForMethod(builder, method.getDeclaringClass(), method);

      String propertyName = getResourcePropertyName(method);

      return new FixedPropertyModel(stateType.getSimpleName(), description, "/" + propertyName);
    });
  }

  private static String getResourcePropertyName(Method method) {

    ResourceProperty annotation = method.getAnnotation(ResourceProperty.class);
    if (StringUtils.isNotBlank(annotation.value())) {
      return annotation.value();
    }

    String name = method.getName();
    if (name.startsWith("get")) {
      return Introspector.decapitalize(name.substring(3));
    }
    if (name.startsWith("is")) {
      return Introspector.decapitalize(name.substring(2));
    }
    return Introspector.decapitalize(name);
  }

  private static Stream<RhymePropertyDocs> crreateDocsFromResourceState(JavaClass apiInterface, JavaProjectBuilder builder, ClassLoader projectClassLoader) {

    Optional<JavaMethod> javaMethod = DocumentationUtils.getMethodsWithAnnotation(apiInterface, ResourceState.class).findFirst();
    if (!javaMethod.isPresent()) {
      return Stream.empty();
    }

    Method method = DocumentationUtils.getMethod(apiInterface, javaMethod.get(), projectClassLoader);
    Class<?> stateType = RxJavaReflectionUtils.getObservableEmissionType(method, RhymeResourceDocs.TYPE_SUPPORT);

    if (TreeNode.class.isAssignableFrom(stateType)) {
      return Stream.of(new FixedPropertyModel("JSON Object",
          "The HAL API Interface for this method uses a generic JSON node, so that the property structure is not specified", "/"));
    }

    Map<String, String> processedClassNames = new HashMap<>();

    return createPropertyDocsRecursively(builder, stateType, "", processedClassNames);
  }

  private static Stream<RhymePropertyDocs> createPropertyDocsRecursively(JavaProjectBuilder builder, Class<?> stateType, String basePointer,
      Map<String, String> processedClassNames) {

    processedClassNames.putIfAbsent(stateType.getName(), basePointer);

    Stream<RhymePropertyDocsImpl> beanProperties = DocumentationUtils.getBeanProperties(stateType)
        .map(property -> new BeanPropertyModel(builder, stateType, property, basePointer));

    Stream<RhymePropertyDocsImpl> fieldProperties = DocumentationUtils.getPublicFields(stateType)
        .map(field -> new FieldPropertyModel(builder, stateType, field, basePointer));

    return Stream.concat(beanProperties, fieldProperties)
        .sorted(Ordering.natural().onResultOf(RhymePropertyDocs::getJsonPointer))
        .flatMap(docs -> handleObjectsAndArrays(docs, processedClassNames));
  }

  private static Stream<RhymePropertyDocs> handleObjectsAndArrays(RhymePropertyDocsImpl docs, Map<String, String> processedClassNames) {

    Class<?> propertyType = docs.getPropertyClass();

    Stream<RhymePropertyDocs> stream = Stream.of(docs);

    if (isSerialisedAsJsonArray(propertyType)) {

      Class<?> elementType = docs.getFirstTypeParameter();

      if (elementType != null && isSerialisedAsJsonObject(elementType)) {

        if (processedClassNames.containsKey(elementType.getName())) {
          String previous = processedClassNames.get(elementType.getName());
          return Stream.of(docs,
              new FixedPropertyModel(elementType.getSimpleName(), "(an object with same properties as " + previous + ")", docs.getJsonPointer() + "/0"));
        }

        return Stream.concat(stream,
            createPropertyDocsRecursively(docs.builder, elementType, docs.getJsonPointer() + "/0", processedClassNames));
      }
    }
    else if (isSerialisedAsJsonObject(propertyType)) {

      if (processedClassNames.containsKey(propertyType.getName())) {
        String previous = processedClassNames.get(propertyType.getName());
        return Stream
            .of(new FixedPropertyModel(docs.getType(), docs.getDescription() + " (an object with same properties as " + previous + ")", docs.getJsonPointer()));
      }

      stream = Stream.concat(stream,
          createPropertyDocsRecursively(docs.builder, propertyType, docs.getJsonPointer(), processedClassNames));
    }


    return stream;
  }

  static class FixedPropertyModel implements RhymePropertyDocs {

    private final String type;
    private final String description;
    private final String jsonPointer;

    public FixedPropertyModel(String type, String description, String jsonPointer) {
      this.type = type;
      this.description = description;
      this.jsonPointer = jsonPointer;
    }

    @Override
    public String getDescription() {
      return description;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public String getJsonPointer() {
      return jsonPointer;
    }

  }

  static class BeanPropertyModel extends RhymePropertyDocsImpl {

    private final PropertyDescriptor property;
    private final Method readMethod;

    BeanPropertyModel(JavaProjectBuilder builder, Class stateType, PropertyDescriptor descriptor, String basePointer) {
      super(builder, stateType, basePointer);
      this.property = descriptor;
      this.readMethod = descriptor.getReadMethod();
    }

    @Override
    protected Class<?> getPropertyClass() {
      return readMethod.getReturnType();
    }

    @Override
    protected Type getPropertyGenericType() {
      return readMethod.getGenericReturnType();
    }

    @Override
    public String getPropertyName() {
      return property.getName();
    }

    @Override
    public String getDescription() {
      String description = DocumentationUtils.findJavaDocForMethod(builder, stateType, readMethod);
      if (StringUtils.isBlank(description)) {
        description = DocumentationUtils.findJavaDocForField(builder, stateType, property.getName());
      }
      return description;
    }
  }

  static class FieldPropertyModel extends RhymePropertyDocsImpl {

    private final Field field;

    FieldPropertyModel(JavaProjectBuilder builder, Class stateType, Field field, String basePointer) {
      super(builder, stateType, basePointer);
      this.field = field;
    }

    @Override
    protected Class<?> getPropertyClass() {
      return field.getType();
    }

    @Override
    protected Type getPropertyGenericType() {
      return field.getGenericType();
    }

    @Override
    public String getPropertyName() {
      return field.getName();
    }

    @Override
    public String getDescription() {
      return DocumentationUtils.findJavaDocForField(builder, stateType, field);
    }
  }

  static Stream<Class<?>> getTypeArguments(Type type) {

    if (!(type instanceof ParameterizedType)) {
      return Stream.empty();
    }

    ParameterizedType observableType = (ParameterizedType)type;

    return Stream.of(observableType.getActualTypeArguments())
        .filter(typeArgument -> !"?".equals(typeArgument.getTypeName()))
        .map(typeArgument -> (Class<?>)typeArgument);
  }

  private static boolean isSerialisedAsJsonArray(Class<?> propertyType) {

    return Collection.class.isAssignableFrom(propertyType) || propertyType.isArray();
  }

  private static boolean isSerialisedAsJsonObject(Class<?> propertyType) {

    return !(isPrimitiveOrWrapper(propertyType)
        || isSerialisedAsJsonArray(propertyType)
        || String.class.isAssignableFrom(propertyType)
        || Map.class.isAssignableFrom(propertyType));
  }
}
