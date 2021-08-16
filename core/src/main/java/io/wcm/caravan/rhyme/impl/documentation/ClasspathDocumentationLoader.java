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
package io.wcm.caravan.rhyme.impl.documentation;

import java.io.IOException;
import java.io.InputStream;

import io.wcm.caravan.rhyme.api.documenation.DocumentationLoader;

public class ClasspathDocumentationLoader implements DocumentationLoader {

  private final ClassLoader classLoader;
  private final String rhymeDocsBaseUrl;

  public ClasspathDocumentationLoader() {
    this.classLoader = getClass().getClassLoader();
    this.rhymeDocsBaseUrl = "";
  }

  public ClasspathDocumentationLoader(ClassLoader classLoader, String rhymeDocsBaseUrl) {
    this.classLoader = classLoader;
    this.rhymeDocsBaseUrl = rhymeDocsBaseUrl;
  }

  @Override
  public String getRhymeDocsBaseUrl() {

    return rhymeDocsBaseUrl;
  }

  @Override
  public InputStream createInputStream(String resourcePath) throws IOException {

    return classLoader.getResourceAsStream(resourcePath);
  }
}
