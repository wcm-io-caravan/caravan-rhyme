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
package io.wcm.caravan.maven.plugins.rhymedocs.templating;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.logging.Log;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.common.base.Charsets;

import io.wcm.caravan.maven.plugins.rhymedocs.model.RhymeApiDocs;
import io.wcm.caravan.maven.plugins.rhymedocs.model.RhymeResourceDocs;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

public class RhymeDocsHtmlRenderer {

  private static final String TEMPLATES_DIRECTORY_NAME = "templates";

  private final Path outputDirectory;
  private final Log log;

  private final Template resourceTemplate;

  public RhymeDocsHtmlRenderer(Path outputDirectory, Log log) throws IOException {

    this.outputDirectory = outputDirectory;
    this.log = log;

    Handlebars handlebars = new Handlebars();

    this.resourceTemplate = handlebars.compile(TEMPLATES_DIRECTORY_NAME + "/resource-docs");
  }


  public void writeHtml(RhymeApiDocs apiDocs) {

    apiDocs.getResourceDocs().forEach(this::generateHtmlFile);
  }

  private void generateHtmlFile(RhymeResourceDocs resourceDocs) {

    String className = resourceDocs.getCanonicalClassName();

    try {
      log.info("Generating HTML API docs for " + className);

      String htmlDocs = resourceTemplate.apply(resourceDocs);

      String fileName = className + ".html";
      Path htmlFile = outputDirectory.resolve(fileName);

      log.debug("Writing " + htmlFile);
      Files.write(htmlFile, htmlDocs.getBytes(Charsets.UTF_8));
    }
    catch (IOException | RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to generate documentation for interface " + className, ex);
    }
  }
}
