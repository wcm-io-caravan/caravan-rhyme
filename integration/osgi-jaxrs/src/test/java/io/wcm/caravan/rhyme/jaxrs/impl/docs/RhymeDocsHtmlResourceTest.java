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
package io.wcm.caravan.rhyme.jaxrs.impl.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.base.Charsets;

@ExtendWith(OsgiContextExtension.class)
@ExtendWith(MockitoExtension.class)
public class RhymeDocsHtmlResourceTest {

  private final OsgiContext context = new OsgiContext();

  @Mock
  private RhymeDocsOsgiBundleSupport docsSupport;

  @Mock
  private UriInfo uriInfo;

  private RhymeDocsHtmlResource resource;

  @BeforeEach
  void setUp() {

    context.registerService(docsSupport);

    // this is not really required for the resource, but we want to at least test this can be activated
    context.registerService(new RhymeDocsJaxRsApplication());

    resource = context.registerInjectActivateService(new RhymeDocsHtmlResource());
  }

  private String getHtmlDocumentation(String fileName) {

    return resource.getHtmlDocumentation(fileName);
  }


  @Test
  void getHtmlDocumentation_should_return_404_if_no_InputStream_available_for_given_filename() throws Exception {

    Throwable ex = catchThrowable(() -> getHtmlDocumentation("Foo.html"));

    assertThat(ex).isInstanceOf(WebApplicationException.class);

    assertThat(((WebApplicationException)ex).getResponse().getStatus())
        .isEqualTo(404);
  }

  @Test
  void getHtmlDocumentation_should_return_html_if_InputStream_available_for_given_filename() throws Exception {

    String expectedHtml = "<föö></föö>";

    when(docsSupport.openResourceStream(anyString()))
        .thenReturn(IOUtils.toInputStream(expectedHtml, Charsets.UTF_8));

    String html = getHtmlDocumentation("Foo.html");

    assertThat(html)
        .isEqualTo(expectedHtml);
  }

}
