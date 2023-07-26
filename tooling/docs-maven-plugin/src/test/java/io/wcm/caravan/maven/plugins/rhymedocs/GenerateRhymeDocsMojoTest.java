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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;

import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;

@ExtendWith(MockitoExtension.class)
class GenerateRhymeDocsMojoTest {

  private static final String GENERATED_RHYME_DOCS = "generated-rhyme-docs";

  @Mock
  MavenProject projectMock;

  @Mock
  Build buildMock;

  @TempDir
  Path tempDir;

  private GenerateRhymeDocsMojoSubclass mojo;

  /**
   * subclassed so that we can set the project and source variables
   */
  class GenerateRhymeDocsMojoSubclass extends GenerateRhymeDocsMojo {


    GenerateRhymeDocsMojoSubclass() {
      super();
      super.project = projectMock;
      super.generatedResourcesDirectory = GENERATED_RHYME_DOCS;
    }

    void setSource(String newSource) {
      source = newSource;
    }
  }

  @BeforeEach
  void setUp() {
    mojo = new GenerateRhymeDocsMojoSubclass();
  }

  /**
   * set up mocks and mojo so that it will find the annotated interfaces from the test sources / classes
   * @throws DependencyResolutionRequiredException
   */
  private void setupMocksForSuccessfulGeneration() throws DependencyResolutionRequiredException {
    when(projectMock.getBuild())
        .thenReturn(buildMock);

    when(projectMock.getCompileClasspathElements())
        .thenReturn(Lists.newArrayList("target/test-classes"));

    mojo.setSource("src/test/java");

    when(buildMock.getDirectory())
        .thenReturn(tempDir.toString());
  }

  private boolean isNotEmptyFile(Path path) {
    try {
      return Files.size(path) > 0;
    }
    catch (IOException ex) {
      throw new AssertionError("Failed to access generated file at " + path, ex);
    }
  }


  @Test
  void execute_should_generate_documentation_if_interfaces_are_found_in_classpath()
      throws DependencyResolutionRequiredException, MojoExecutionException, MojoFailureException, IOException {

    setupMocksForSuccessfulGeneration();

    mojo.execute();

    Path generatedDir = tempDir.resolve(GENERATED_RHYME_DOCS);

    assertThat(Files.isDirectory(generatedDir))
        .isTrue();

    Stream<Path> generatedFiles = Files.list(generatedDir)
        .filter(path -> path.getFileName().toString().endsWith(".html"));

    assertThat(generatedFiles)
        .isNotEmpty()
        .allMatch(this::isNotEmptyFile);
  }

  @Test
  void execute_should_add_generated_documentation_to_project_resources()
      throws DependencyResolutionRequiredException, MojoExecutionException, MojoFailureException {

    setupMocksForSuccessfulGeneration();

    mojo.execute();

    Path generatedDir = tempDir.resolve(GENERATED_RHYME_DOCS);

    ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
    verify(buildMock).addResource(resourceCaptor.capture());
    Resource addedResource = resourceCaptor.getValue();

    assertThat(addedResource.getDirectory())
        .isEqualTo(generatedDir.toString());

    assertThat(addedResource.getTargetPath())
        .isEqualTo(RhymeDocsSupport.FOLDER);
  }

  @Test
  void execute_should_fail_if_there_are_no_classpath_elements() throws DependencyResolutionRequiredException {

    when(projectMock.getCompileClasspathElements())
        .thenReturn(Collections.emptyList());

    mojo.setSource("src/main/java");

    MojoExecutionException ex = assertThrows(MojoExecutionException.class, () -> mojo.execute());

    assertThat(ex)
        .hasMessageStartingWith("Generating Rhyme documentation failed")
        .hasRootCauseMessage(GenerateRhymeDocsMojo.NO_INTERFACES_FOUND_MSG);

    verifyNoInteractions(buildMock);
  }

  @Test
  void execute_should_fail_if_there_are_invalid_classpath_elements() throws DependencyResolutionRequiredException {

    when(projectMock.getCompileClasspathElements())
        .thenReturn(Lists.newArrayList("foo"));

    MojoExecutionException ex = assertThrows(MojoExecutionException.class, () -> mojo.execute());

    assertThat(ex)
        .hasMessageStartingWith("Generating Rhyme documentation failed")
        .hasRootCauseInstanceOf(NoSuchElementException.class);

    verifyNoInteractions(buildMock);
  }
}
