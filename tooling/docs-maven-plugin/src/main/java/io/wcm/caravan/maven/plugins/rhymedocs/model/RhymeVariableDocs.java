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

import static io.wcm.caravan.maven.plugins.rhymedocs.model.DocumentationUtils.findJavaDocForField;
import static io.wcm.caravan.maven.plugins.rhymedocs.model.DocumentationUtils.findJavaDocForMethod;

import java.util.stream.Collectors;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaMethod;

import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.impl.reflection.TemplateVariableDetection.TemplateVariableWithTypeInfo;

/**
 * provides information about a template variable from a {@link Related} method definition
 */
public class RhymeVariableDocs {

  private final String name;
  private final String type;
  private final String description;

  public RhymeVariableDocs(JavaProjectBuilder builder, TemplateVariableWithTypeInfo variable, JavaMethod javaMethod) {

    this.name = variable.getName();
    this.type = variable.getType().getSimpleName();

    if (variable.getDtoMethod() != null) {
      this.description = findJavaDocForMethod(builder, variable.getDtoClass(), variable.getDtoMethod());
    }
    else if (variable.getDtoField() != null) {
      this.description = findJavaDocForField(builder, variable.getDtoClass(), variable.getDtoField());
    }
    else {
      this.description = findJavaDocForNamedParameter(javaMethod, name);
    }
  }

  private String findJavaDocForNamedParameter(JavaMethod javaMethod, String paramName) {

    return javaMethod.getTagsByName("param", true).stream()
        .filter(tag -> paramName.equals(tag.getParameters().get(0)))
        .flatMap(tag -> tag.getParameters().stream().skip(1))
        .collect(Collectors.joining(" "));
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
