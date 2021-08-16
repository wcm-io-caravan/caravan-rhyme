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
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;


@Mojo(name = "generate-rhyme-docs", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
    requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true, threadSafe = true)
public class GenerateRhymeDocsMojo extends AbstractMojo {

  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  /**
   * Path containing the java source files.
   */
  @Parameter(defaultValue = "${basedir}/src/main/java")
  private String source;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    try {

      ClassLoader compileClassLoader = getCompileClassLoader();

      RhymeApiDocs apiDocs = new RhymeApiDocs(Paths.get(source), compileClassLoader);

      Path outputDirectory = createOutputDirectory();

      RhymeDocsHtmlRenderer renderer = new RhymeDocsHtmlRenderer(outputDirectory, getLog());
      renderer.writeHtml(apiDocs);

      addResourcesToClassPath(outputDirectory);
    }
    catch (Throwable ex) {
      throw new MojoExecutionException("Generating Rhyme documentation failed: " + ex.getMessage(), ex);
    }
  }

  private URLClassLoader getCompileClassLoader() throws DependencyResolutionRequiredException {

    URL[] classPathElementUrls = project.getCompileClasspathElements().stream()
        .map(this::createFileUrl)
        .peek(url -> getLog().info("using classpath element " + url))
        .toArray(URL[]::new);

    return URLClassLoader.newInstance(classPathElementUrls, getClass().getClassLoader());
  }

  private URL createFileUrl(String path) {
    try {
      return new File(path).toURI().toURL();
    }
    catch (MalformedURLException ex) {
      throw new RuntimeException(ex);
    }
  }

  private Path createOutputDirectory() throws IOException {

    // generate the .html files directly into a folder within the target/classes directory

    // it would be better to generate them in a temporary folder right below target, but this
    // was the easiest way to ensure they are also picked up by the spring-boot-maven-plugin when
    // it builds the fat jar with all dependencies
    Path outputDirectory = Paths.get(project.getBuild().getDirectory(), "classes", RhymeDocsSupport.FOLDER);

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
