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
package io.wcm.caravan.rhyme.testing.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.relations.StandardRelations;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import wiremock.com.github.jknack.handlebars.internal.lang3.math.NumberUtils;

class HalCrawlerTest {

  @Test
  void should_crawl_from_root_resource() {

    MockResourceChainLoader loader = new MockResourceChainLoader()
        .withLimit(5);

    HalCrawler crawler = new HalCrawler(loader);

    assertThat(crawler.getAllResponses())
        .hasSize(5)
        .extracting(HalResponse::getUri)
        .containsExactly("/", "/1", "/2", "/3", "/4");
  }

  @Test
  void should_crawl_from_custom_entry_point() {

    MockResourceChainLoader loader = new MockResourceChainLoader()
        .withLimit(5);

    HalCrawler crawler = new HalCrawler(loader)
        .withEntryPoint("/1");

    assertThat(crawler.getAllResponses())
        .hasSize(4)
        .extracting(HalResponse::getUri)
        .containsExactly("/1", "/2", "/3", "/4");
  }

  @Test
  void should_ignore_link_templates() {

    MockResourceChainLoader loader = new MockResourceChainLoader()
        .withLinkTemplate()
        .withLimit(5);

    HalCrawler crawler = new HalCrawler(loader);

    assertThat(crawler.getAllResponses())
        .hasSize(5)
        .extracting(HalResponse::getUri)
        .containsExactly("/", "/1", "/2", "/3", "/4");
  }

  @Test
  void should_respect_default_limit() {

    MockResourceChainLoader loader = new MockResourceChainLoader();

    HalCrawler crawler = new HalCrawler(loader);

    assertThat(crawler.getAllResponses())
        .hasSize(1000);
  }

  @Test
  void should_respect_custom_limit() {

    MockResourceChainLoader loader = new MockResourceChainLoader();

    HalCrawler crawler = new HalCrawler(loader)
        .withLimit(10);

    assertThat(crawler.getAllResponses())
        .hasSize(10)
        .extracting(HalResponse::getUri)
        .containsExactly("/", "/1", "/2", "/3", "/4", "/5", "/6", "/7", "/8", "/9");
  }

  @Test
  void should_ignore_relations() {

    MockResourceChainLoader loader = new MockResourceChainLoader();

    HalCrawler crawler = new HalCrawler(loader)
        .withIgnoredRelations(ImmutableList.of(StandardRelations.NEXT));

    assertThat(crawler.getAllResponses())
        .hasSize(1)
        .extracting(HalResponse::getUri)
        .containsExactly("/");
  }

  @Test
  void should_filter_links_with_custom_content_type() {

    MockResourceChainLoader loader = new MockResourceChainLoader()
        .withAdditionalLink(new Link("/foo").setType("text/html"));

    HalCrawler crawler = new HalCrawler(loader)
        .withLimit(10);

    assertThat(crawler.getAllResponses())
        .hasSize(10)
        .extracting(HalResponse::getUri)
        .doesNotContain("/foo");
  }

  @Test
  void should_not_filter_links_with_hal_content_type() {

    MockResourceChainLoader loader = new MockResourceChainLoader()
        .withAdditionalLink(new Link("/foo").setType(HalResource.CONTENT_TYPE));

    HalCrawler crawler = new HalCrawler(loader)
        .withLimit(10);

    assertThat(crawler.getAllResponses())
        .hasSize(10)
        .extracting(HalResponse::getUri)
        .contains("/foo");
  }

  @Test
  void should_not_crawl_multiple_times() {

    MockResourceChainLoader loader = new MockResourceChainLoader()
        .withLimit(5);

    HalCrawler crawler = new HalCrawler(loader);

    assertThat(crawler.getAllResponses())
        .hasSize(5);

    assertThat(crawler.getAllResponses())
        .hasSize(5)
        .extracting(HalResponse::getUri)
        .containsExactly("/", "/1", "/2", "/3", "/4");
  }

  @Test
  void should_modify_uris() {

    MockResourceChainLoader loader = new MockResourceChainLoader()
        .withLimit(5);

    HalCrawler crawler = new HalCrawler(loader)
        .withModifiedUrls(uri -> uri.contains("?foo") ? uri : uri + "?foo");

    assertThat(crawler.getAllResponses())
        .extracting(HalResponse::getUri)
        .containsExactly("/?foo", "/1?foo", "/2?foo", "/3?foo", "/4?foo")
        .hasSize(5);

    assertThat(crawler.getAllResponses())
        .hasSize(5)
        .extracting(HalResponse::getUri)
        .containsExactly("/?foo", "/1?foo", "/2?foo", "/3?foo", "/4?foo");
  }

  class MockResourceChainLoader implements HalResourceLoader {

    private Integer limit;

    private boolean includeLinkTemplate;

    private List<Link> additionalLinks = new ArrayList<>();

    MockResourceChainLoader withLinkTemplate() {
      includeLinkTemplate = true;
      return this;
    }

    MockResourceChainLoader withLimit(int maxNumResources) {
      this.limit = maxNumResources;
      return this;
    }


    MockResourceChainLoader withAdditionalLink(Link link) {
      this.additionalLinks.add(link);
      return this;
    }

    @Override
    public Single<HalResponse> getHalResource(String uri) {

      HalResource body = new HalResource(uri);
      int nextIndex = getNextIndex(uri);
      if (limit == null || nextIndex < limit) {
        body.addLinks(StandardRelations.NEXT, new Link("/" + nextIndex));
      }

      if (includeLinkTemplate) {
        body.addLinks("foo:template", new Link("/{index}"));
        body.addLinks("curies", new Link("/docs/foo").setName("foo"));
      }

      body.addLinks("additional", additionalLinks);

      return Single.just(new HalResponse()
          .withStatus(200)
          .withUri(uri)
          .withBody(body));
    }

    private int getNextIndex(String currentUri) {

      String path = currentUri.contains("?") ? StringUtils.substringBefore(currentUri, "?") : currentUri;
      if (path.length() == 1) {
        return 1;
      }
      int currentIndex = NumberUtils.toInt(path.substring(1));

      return currentIndex + 1;
    }
  }
}
