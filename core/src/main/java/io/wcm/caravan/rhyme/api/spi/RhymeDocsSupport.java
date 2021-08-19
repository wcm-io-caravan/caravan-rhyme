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
package io.wcm.caravan.rhyme.api.spi;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.osgi.annotation.versioning.ConsumerType;

import com.google.common.base.Charsets;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;

/**
 * SPI interface to customize the integration of HTML documentation generated with
 * the rhyme-docs-maven-plugin
 */
@ConsumerType
public interface RhymeDocsSupport {

  /** the folder in the classpath where the generated HTML files are located */
  String FOLDER = "RHYME-DOCS-INF";

  /**
   * This method allows the framework to construct the full URL from which the documentation will be served,
   * which is important for the curie links to be constructed correctly
   * @return the URL to be prepended to the HTML file name
   */
  String getRhymeDocsBaseUrl();

  /**
   * Loads the HTML documentation file from the given path in the class path
   * @param resourcePath an absolute path (starting with "/RHYME-DOCS-INF/") to the HTML file to be loaded
   * @return the input stream that provides the file's content (or null if no such file exists)
   * @throws IOException if loading of the file fails
   */
  InputStream openResourceStream(String resourcePath) throws IOException;

  /**
   * Extract the HTML documentation that was generated with the rhyme-docs-maven-plugin from the classpath.
   * This default implementation contains additional logic for constructing the full resource path,
   * and some error handling code (to ensure that a {@link HalApiServerException} with a reasonable
   * status code will be thrown if HTML could not be loaded)
   * @param docsSupport the instance that implements the {@link #openResourceStream(String)} function
   * @param fileName the name of the generated file, usually the fully qualified class name of a
   *          {@link HalApiInterface}, followed by ".html"
   * @return the content of the HTML documentation file (with UTF-8 encoding)
   * @throws HalApiServerException if the documentation could not be found (404 status) or failed to load (500 status)
   */
  static String loadGeneratedHtml(RhymeDocsSupport docsSupport, String fileName) {

    String resourcePath = "/" + RhymeDocsSupport.FOLDER + "/" + fileName;

    try (InputStream is = docsSupport.openResourceStream(resourcePath)) {
      if (is == null) {
        throw new HalApiServerException(404, "No HTML documentation was generated for " + fileName);
      }
      return IOUtils.toString(is, Charsets.UTF_8);
    }
    catch (HalApiServerException ex) {
      throw ex;
    }
    catch (IOException | RuntimeException ex) {
      throw new HalApiServerException(500, "Failed to load documentation from " + fileName, ex);
    }
  }
}
