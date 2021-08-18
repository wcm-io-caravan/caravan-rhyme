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

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;

public class RhymeApiDocs {

  private final JavaProjectBuilder builder = new JavaProjectBuilder();

  private final List<JavaClass> apiInterfaces;
  private final ClassLoader projectClassLoader;

  public RhymeApiDocs(Path sourcePath, ClassLoader projectClassLoader) {

    this.builder.addSourceTree(sourcePath.toFile());

    this.apiInterfaces = findHalApiInterfaces();
    this.projectClassLoader = projectClassLoader;
  }

  private List<JavaClass> findHalApiInterfaces() {

    return builder.getSources().stream()
        .flatMap(javaSource -> javaSource.getClasses().stream())
        .filter(javaClass -> DocumentationUtils.hasAnnotation(javaClass, HalApiInterface.class))
        .collect(Collectors.toList());
  }

  public List<RhymeResourceDocs> getResourceDocs() {

    return apiInterfaces.stream()
        .map(apiInterface -> new RhymeResourceDocs(builder, apiInterface, projectClassLoader))
        .collect(Collectors.toList());
  }
}
