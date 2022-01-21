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
package io.wcm.caravan.rhyme.spring.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.examples.spring.hypermedia.SpringRhymeHypermediaApplication;
import io.wcm.caravan.rhyme.testing.client.HalCrawler;
import io.wcm.caravan.rhyme.testing.spring.MockMvcHalResourceLoaderConfiguration;

@SpringBootTest(classes = SpringRhymeHypermediaApplication.class)
@Import(MockMvcHalResourceLoaderConfiguration.class)
class SpringRhymeDocsIT {

  @Autowired
  private HalResourceLoader mockMvcResourceLoader;

  private final MockMvc mockMvc;

  public SpringRhymeDocsIT(@Autowired WebApplicationContext applicationContext) {

    this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
  }

  @Test
  void all_curies_links_should_lead_to_a_html_document() throws Exception {

    HalCrawler crawler = new HalCrawler(mockMvcResourceLoader);

    List<String> hrefsFromAllCuriesLinks = crawler.getAllResponses().stream()
        .map(HalResponse::getBody)
        .flatMap(hal -> hal.getLinks("curies").stream())
        .map(Link::getHref)
        .distinct()
        .collect(Collectors.toList());

    assertThat(hrefsFromAllCuriesLinks).as("hrefs from all curies links")
        .isNotEmpty();

    for (String uri : hrefsFromAllCuriesLinks) {

      MvcResult result = mockMvc.perform(get(uri)).andReturn();
      MockHttpServletResponse response = result.getResponse();

      assertThat(response.getStatus()).as("status code of " + uri)
          .isEqualTo(200);

      assertThat(response.getContentType()).as("content type of " + uri)
          .isEqualTo("text/html;charset=UTF-8");

      assertThat(response.getContentAsString())
          .isNotBlank();
    }
  }
}
