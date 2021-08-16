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
package io.wcm.caravan.rhyme.api.documenation;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Charsets;

import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.impl.documentation.ClasspathDocumentationLoader;

public interface DocumentationLoader {

  String FOLDER = "RHYME-DOCS-INF";

  String getRhymeDocsBaseUrl();

  InputStream createInputStream(String resourcePath) throws IOException;

  static DocumentationLoader fromClassPath(ClassLoader classLoader, String docUrlPrefix) {

    return new ClasspathDocumentationLoader(classLoader, docUrlPrefix);
  }

  default String loadFrom(String fileName) {

    String resourcePath = "/" + DocumentationLoader.FOLDER + "/" + fileName;

    try (InputStream is = createInputStream(resourcePath)) {
      if (is == null) {
        throw new HalApiServerException(404, "No HTML documentation was generated for " + fileName);
      }
      return IOUtils.toString(is, Charsets.UTF_8);
    }
    catch (HalApiServerException ex) {
      throw ex;
    }
    catch (IOException | RuntimeException ex) {
      throw new RuntimeException("Failed to load documentation from " + fileName, ex);
    }
  }

}
