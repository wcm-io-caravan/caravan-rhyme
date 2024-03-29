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
package io.wcm.caravan.maven.plugins.rhymedocs;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import io.wcm.caravan.maven.plugins.rhymedocs.model.RhymeApiDocs;
import io.wcm.caravan.maven.plugins.rhymedocs.templating.RhymeDocsHtmlRenderer;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;


@Mojo(name = "generate-rhyme-docs", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
    requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true, threadSafe = true)
public class GenerateRhymeDocsMojo extends AbstractMojo {

  static final String NO_INTERFACES_FOUND_MSG = "No interfaces annotated with @HalApiInterface were found in the sources for this project";

  @Parameter(property = "project", required = true, readonly = true)
  protected MavenProject project;

  /**
   * Path containing the java source files.
   */
  @Parameter(defaultValue = "${basedir}/src/main/java")
  protected String source;

  @Parameter(defaultValue = "generated-rhyme-docs")
  protected String generatedResourcesDirectory;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    try {

      ClassLoader compileClassLoader = getCompileClassLoader();

      RhymeApiDocs apiDocs = new RhymeApiDocs(Paths.get(source), compileClassLoader);

      if (apiDocs.getResourceDocs().isEmpty()) {
        throw new MojoExecutionException(NO_INTERFACES_FOUND_MSG);
      }

      Path outputDirectory = createOutputDirectory();

      RhymeDocsHtmlRenderer renderer = new RhymeDocsHtmlRenderer(outputDirectory, getLog());
      renderer.writeHtml(apiDocs);

      addResourcesToClassPath(outputDirectory);
    }
    catch (Exception ex) {
      throw new MojoExecutionException("Generating Rhyme documentation failed: " + ex.getMessage(), ex);
    }
  }

  private URLClassLoader getCompileClassLoader() throws DependencyResolutionRequiredException {

    URL[] classPathElementUrls = project.getCompileClasspathElements().stream()
        .map(this::createFileUrl)
        .peek(url -> getLog().debug("adding " + url + " to class loader"))
        .toArray(URL[]::new);

    return URLClassLoader.newInstance(classPathElementUrls, getClass().getClassLoader());
  }

  private URL createFileUrl(String path) {
    try {
      File file = new File(path);
      if (!file.exists()) {
        throw new NoSuchElementException("The classpath element " + file.getAbsolutePath() + " does not exist");
      }
      return file.toURI().toURL();
    }
    catch (MalformedURLException | RuntimeException ex) {
      throw new HalApiDeveloperException("Failed to create URL from path " + path, ex);
    }
  }

  private Path createOutputDirectory() throws IOException {

    Path outputDirectory = Paths.get(project.getBuild().getDirectory(), generatedResourcesDirectory);

    Files.createDirectories(outputDirectory);

    return outputDirectory;
  }

  private void addResourcesToClassPath(Path outputDirectory) {

    // construct resource
    Resource resource = new Resource();
    resource.setDirectory(outputDirectory.toString());
    resource.setTargetPath(RhymeDocsSupport.FOLDER);

    // add to build
    Build build = project.getBuild();
    build.addResource(resource);
    getLog().info("Added resource: " + resource.getDirectory() + " -> " + resource.getTargetPath());
  }

}
