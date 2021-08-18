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

import java.util.List;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;

import io.wcm.caravan.rhyme.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;

public class RhymeResourceDocs {

  static final HalApiTypeSupport TYPE_SUPPORT = new DefaultHalApiTypeSupport();

  private final JavaClass apiInterface;

  private final List<RhymePropertyDocs> properties;
  private final List<RhymeRelatedMethodDocs> relations;

  public RhymeResourceDocs(JavaClass apiInterface, JavaProjectBuilder builder, ClassLoader projectClassLoader) {

    this.apiInterface = apiInterface;

    this.properties = RhymePropertyDocs.create(apiInterface, builder, projectClassLoader);
    this.relations = RhymeRelatedMethodDocs.create(apiInterface, builder, projectClassLoader);
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

  public List<RhymePropertyDocs> getProperties() {

    return properties;
  }

  public List<RhymeRelatedMethodDocs> getRelations() {

    return relations;
  }
}
