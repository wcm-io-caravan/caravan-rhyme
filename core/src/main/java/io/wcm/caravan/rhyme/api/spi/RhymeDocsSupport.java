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

import org.osgi.annotation.versioning.ConsumerType;

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
}
