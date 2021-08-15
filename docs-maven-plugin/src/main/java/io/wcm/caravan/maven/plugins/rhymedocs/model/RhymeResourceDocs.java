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

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Ordering;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.RxJavaReflectionUtils;

public class RhymeResourceDocs {

  private static final HalApiTypeSupport TYPE_SUPPORT = new DefaultHalApiTypeSupport();

  private final JavaClass apiInterface;

  private final ClassLoader projectClassLoader;

  private final List<RhymeRelatedMethodDocs> relations;

  public RhymeResourceDocs(JavaClass apiInterface, ClassLoader projectClassLoader) {

    this.apiInterface = apiInterface;
    this.projectClassLoader = projectClassLoader;

    this.relations = createRelatedMethodDocs(apiInterface);
  }

  private List<RhymeRelatedMethodDocs> createRelatedMethodDocs(JavaClass apiInterface) {

    return apiInterface.getMethods(true).stream()
        .filter(method -> AnnotationUtils.hasAnnotation(method, Related.class))
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

    private RhymeRelatedMethodDocs(JavaMethod javaMethod) {

      Method method = AnnotationUtils.getMethod(apiInterface, javaMethod, projectClassLoader);

      this.relation = method.getAnnotation(Related.class).value();
      this.description = javaMethod.getComment();

      this.relatedResourceType = getRelatedResourceType(method);
      this.cardinality = getCardinality(method);
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
  }
}
