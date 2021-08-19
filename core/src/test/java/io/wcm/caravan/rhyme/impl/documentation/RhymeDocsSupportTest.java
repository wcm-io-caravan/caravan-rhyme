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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Charsets;

import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;


@ExtendWith(MockitoExtension.class)
public class RhymeDocsSupportTest {

  @Mock
  private RhymeDocsSupport mock;

  private RhymeDocsSupport docsSupport;

  @BeforeEach
  void setUp() {
    docsSupport = new RhymeDocsSupport() {

      @Override
      public InputStream openResourceStream(String resourcePath) throws IOException {
        return mock.openResourceStream(resourcePath);
      }

      @Override
      public String getRhymeDocsBaseUrl() {
        return mock.getRhymeDocsBaseUrl();
      }
    };
  }

  private static InputStream inputStreamWith(String string) {

    return new ByteArrayInputStream(string.getBytes(Charsets.UTF_8));
  }

  @Test
  public void loadGeneratedHtmlFrom_should_prepend_folder_to_resource_path() throws Exception {

    ArgumentCaptor<String> resourcePath = ArgumentCaptor.forClass(String.class);

    when(mock.openResourceStream(resourcePath.capture()))
        .thenReturn(inputStreamWith("<html />"));

    RhymeDocsSupport.loadGeneratedHtml(docsSupport, "Foo.html");

    assertThat(resourcePath.getValue())
        .isEqualTo("/" + RhymeDocsSupport.FOLDER + "/Foo.html");
  }

  @Test
  public void loadGeneratedHtmlFrom_should_parse_String_as_utf_8() throws Exception {

    String expectedHtml = "<föö />";

    when(mock.openResourceStream(anyString()))
        .thenReturn(inputStreamWith(expectedHtml));

    String actualHtml = RhymeDocsSupport.loadGeneratedHtml(docsSupport, "Foo.html");

    assertThat(actualHtml)
        .isEqualTo(expectedHtml);
  }

  @Test
  public void loadGeneratedHtmlFrom_should_close_stream() throws Exception {

    InputStream stream = spy(inputStreamWith("<föö />"));

    when(mock.openResourceStream(anyString()))
        .thenReturn(stream);

    RhymeDocsSupport.loadGeneratedHtml(docsSupport, "Foo.html");

    verify(stream).close();
  }

  @Test
  public void loadGeneratedHtmlFrom_should_throw_404_exception_if_no_docs_were_found() throws Exception {

    when(mock.openResourceStream(anyString()))
        .thenReturn(null);

    Throwable ex = catchThrowable(() -> RhymeDocsSupport.loadGeneratedHtml(docsSupport, "Foo.html"));

    assertThat(ex)
        .isInstanceOf(HalApiServerException.class)
        .hasMessage("No HTML documentation was generated for Foo.html");

    assertThat(((HalApiServerException)ex).getStatusCode())
        .isEqualTo(404);
  }

  @Test
  public void loadGeneratedHtmlFrom_should_throw_500_exception_if_IOException_is_caught() throws Exception {

    InputStream stream = mock(InputStream.class);

    when(stream.read(any(), anyInt(), anyInt()))
        .thenThrow(IOException.class);

    when(mock.openResourceStream(anyString()))
        .thenReturn(stream);

    Throwable ex = catchThrowable(() -> RhymeDocsSupport.loadGeneratedHtml(docsSupport, "Foo.html"));

    assertThat(ex)
        .isInstanceOf(HalApiServerException.class)
        .hasMessage("Failed to load documentation from Foo.html")
        .hasCauseInstanceOf(IOException.class);

    assertThat(((HalApiServerException)ex).getStatusCode())
        .isEqualTo(500);
  }

}
