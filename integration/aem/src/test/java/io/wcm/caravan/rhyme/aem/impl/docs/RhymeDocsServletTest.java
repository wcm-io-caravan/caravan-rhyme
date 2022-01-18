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
package io.wcm.caravan.rhyme.aem.impl.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Charsets;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextBuilder;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;


@ExtendWith(AemContextExtension.class)
@ExtendWith(MockitoExtension.class)
public class RhymeDocsServletTest {

  private AemContext context = new AemContextBuilder().build();

  @Mock
  private RhymeDocsOsgiBundleSupport docsSupport;

  private RhymeDocsServlet servlet;

  @BeforeEach
  void setUp() {

    context.registerService(docsSupport);

    servlet = context.registerInjectActivateService(new RhymeDocsServlet());
  }

  private void getHtmlDocumentation(String suffix) throws ServletException, IOException {

    context.requestPathInfo().setResourcePath("/content");
    context.requestPathInfo().setSelectorString(RhymeDocsServlet.SELECTOR);
    context.requestPathInfo().setExtension(RhymeDocsServlet.EXTENSION);
    context.requestPathInfo().setSuffix(suffix);

    servlet.doGet(context.request(), context.response());
  }

  @Test
  void doGet_should_return_404_if_no_InputStream_available_for_given_filename() throws ServletException, IOException {

    getHtmlDocumentation("/Foo.html");

    assertThat(context.response().getStatus())
        .isEqualTo(404);
  }

  @Test
  void doGet_should_return_400_if_no_suffix_was_prent_in_request() throws ServletException, IOException {

    getHtmlDocumentation(null);

    assertThat(context.response().getStatus())
        .isEqualTo(400);
  }

  @Test
  void doGet_should_return_400_if_relative_path_was_prerent_in_request() throws ServletException, IOException {

    getHtmlDocumentation("/../Foo.html");

    assertThat(context.response().getStatus())
        .isEqualTo(400);
  }

  @Test
  void doGet_should_return_400_if_relative_suffix_was_prerent_in_request() throws ServletException, IOException {

    getHtmlDocumentation("Foo.html");

    assertThat(context.response().getStatus())
        .isEqualTo(400);
  }


  @Test
  void doGet_should_return_200_with_html_if_InputStream_available_for_given_filename() throws IOException, ServletException {

    String expectedHtml = "<föö></föö>";

    when(docsSupport.openResourceStream(anyString()))
        .thenReturn(IOUtils.toInputStream(expectedHtml, Charsets.UTF_8));

    getHtmlDocumentation("/Foo.html");

    assertThat(context.response().getStatus())
        .isEqualTo(200);

    assertThat(context.response().getContentType())
        .isEqualTo("text/html; charset=UTF-8");

    assertThat(context.response().getOutputAsString())
        .isEqualTo(expectedHtml);
  }
}
