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

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class GenerateRhymeDocsMojoTest {

  @Mock
  MavenProject projectMock;

  @Mock
  Build buildMock;

  @TempDir
  Path tempDir;

  private GenerateRhymeDocsMojo mojo;

  /**
   * subclassed so that we can set the project and source variables
   */
  class GenerateRhymeDocsMojoSubclass extends GenerateRhymeDocsMojo {

    GenerateRhymeDocsMojoSubclass() {
      super();
      super.project = projectMock;
      super.source = "src/main/java";
      super.generatedResourcesDirectory = "generated-rhyme-docs";
    }
  }

  @BeforeEach
  void setUp() {
    mojo = new GenerateRhymeDocsMojoSubclass();
  }

  @Test
  public void execute_should_fail_if_there_are_no_classpath_elements() throws Exception {

    Throwable ex = catchThrowable(() -> mojo.execute());

    Assertions.assertThat(ex)
        .isInstanceOf(MojoExecutionException.class)
        .hasMessageStartingWith("Generating Rhyme documentation failed")
        .hasRootCauseMessage(GenerateRhymeDocsMojo.NO_INTERFACES_FOUND_MSG);

    verify(buildMock, never()).addResource(any());
  }

  @Test
  public void execute_should_fail_if_there_are_invalid_classpath_elements() throws Exception {

    when(projectMock.getCompileClasspathElements())
        .thenReturn(Lists.newArrayList("foo", null));

    Throwable ex = catchThrowable(() -> mojo.execute());

    Assertions.assertThat(ex)
        .isInstanceOf(MojoExecutionException.class)
        .hasMessageStartingWith("Generating Rhyme documentation failed");
  }
}
