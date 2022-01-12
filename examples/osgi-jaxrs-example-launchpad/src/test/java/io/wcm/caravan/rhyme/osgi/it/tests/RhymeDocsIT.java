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
package io.wcm.caravan.rhyme.osgi.it.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.base.Charsets;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.osgi.it.IntegrationTestEnvironment;
import io.wcm.caravan.rhyme.osgi.it.extensions.WaitForServerStartupExtension;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;

@ExtendWith({ WaitForServerStartupExtension.class })
public class RhymeDocsIT {

  private final ExamplesEntryPointResource entryPoint = IntegrationTestEnvironment.createEntryPointProxy();

  @Test
  public void entry_point_should_contain_curies_link() {

    HalResource hal = entryPoint.asHalResource();

    List<Link> curies = hal.getLinks("curies");

    assertThat(curies).hasSize(1);
    assertThat(curies.get(0).getName()).isEqualTo("examples");
  }

  @Test
  public void html_docs_should_be_served_on_URI_from_curie_link() throws IOException {

    HalResource hal = entryPoint.asHalResource();

    Link link = hal.getLink("curies");
    String hrefWithoutFragment = StringUtils.substringBefore(link.getHref(), "#");
    URI uri = URI.create(hrefWithoutFragment);

    URLConnection connection = uri.toURL().openConnection();
    connection.connect();

    assertThat(connection.getContentType()).isEqualTo("text/html;charset=utf-8");

    String content = IOUtils.toString(connection.getInputStream(), Charsets.UTF_8.name()).trim();

    // don't make too many assumption about the actual documentation content, but just ensure that
    // this is not a completely unrelated HTML document
    assertThat(content)
        .startsWith("<html>")
        .contains(ExamplesEntryPointResource.class.getSimpleName())
        .contains("id=\"examples:caching\"");
  }

}
