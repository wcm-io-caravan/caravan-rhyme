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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.wcm.caravan.maven.plugins.rhymedocs.model.RhymeApiDocs;
import io.wcm.caravan.maven.plugins.rhymedocs.model.RhymeApiDocsTest;


public class RhymeDocsHtmlRendererTest {

  private static final int NUM_TEST_INTERFACES = RhymeApiDocsTest.TEST_INTERFACES.length;

  private static RhymeApiDocs apiDocs;

  @TempDir
  Path tempDir;

  private RhymeDocsHtmlRenderer renderer;

  @BeforeAll
  static void createApiDocs() {

    apiDocs = new RhymeApiDocs(Paths.get("src/test/java"), RhymeApiDocsTest.class.getClassLoader());

    assertThat(apiDocs.getResourceDocs())
        .hasSize(NUM_TEST_INTERFACES);
  }

  @BeforeEach
  void setUp() throws IOException {

    Log log = Mockito.mock(Log.class);

    renderer = new RhymeDocsHtmlRenderer(tempDir, log);
  }

  @Test
  public void writeHtml_should_generate_one_file_for_each_class() throws Exception {

    renderer.writeHtml(apiDocs);

    long numFiles = Files.list(tempDir).count();
    assertThat(numFiles).isEqualTo(NUM_TEST_INTERFACES);
  }

  @Test
  public void writeHtml_should_use_fully_qualified_class_name_for_filename() throws Exception {

    renderer.writeHtml(apiDocs);

    String[] expectedFileNames = Stream.of(RhymeApiDocsTest.TEST_INTERFACES)
        .map(clazz -> clazz.getCanonicalName() + ".html")
        .toArray(String[]::new);

    List<String> actualFileNames = Files.list(tempDir)
        .map(path -> path.getFileName().toString())
        .collect(Collectors.toList());

    assertThat(actualFileNames).containsExactlyInAnyOrder(expectedFileNames);
  }

  @Test
  public void writeHtml_should_render_non_empty_html_files() throws Exception {

    renderer.writeHtml(apiDocs);

    Path firstFile = Files.list(tempDir).findFirst().orElseThrow(() -> new NoSuchElementException());

    List<String> lines = Files.readAllLines(firstFile);

    assertThat(lines)
        .isNotEmpty()
        .contains("<html>")
        .contains("</html>");
  }

  @Test
  public void writeHtml_fails_if_directory_doesnt_exist() throws Exception {

    Files.delete(tempDir);

    Throwable ex = catchThrowable(() -> renderer.writeHtml(apiDocs));

    assertThat(ex).isInstanceOf(RuntimeException.class)
        .hasMessageStartingWith("Failed to generate documentation")
        .hasCauseInstanceOf(NoSuchFileException.class);
  }
}
