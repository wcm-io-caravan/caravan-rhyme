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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.relations.StandardRelations;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import wiremock.com.github.jknack.handlebars.internal.lang3.math.NumberUtils;

public class HalCrawlerTest {

  @Test
  public void should_crawl_from_root_resource() throws Exception {

    EndlessLoopHalResourceLoader loader = new EndlessLoopHalResourceLoader()
        .withLimit(5);

    HalCrawler crawler = new HalCrawler(loader);

    assertThat(crawler.getAllResponses())
        .hasSize(5)
        .extracting(HalResponse::getUri)
        .containsExactly("/", "/1", "/2", "/3", "/4");
  }

  @Test
  public void should_crawl_from_custom_entry_point() throws Exception {

    EndlessLoopHalResourceLoader loader = new EndlessLoopHalResourceLoader()
        .withLimit(5);

    HalCrawler crawler = new HalCrawler(loader)
        .withEntryPoint("/1");

    assertThat(crawler.getAllResponses())
        .hasSize(4)
        .extracting(HalResponse::getUri)
        .containsExactly("/1", "/2", "/3", "/4");
  }

  @Test
  public void should_ignore_link_templates() throws Exception {

    EndlessLoopHalResourceLoader loader = new EndlessLoopHalResourceLoader()
        .withLinkTemplate()
        .withLimit(5);

    HalCrawler crawler = new HalCrawler(loader);

    assertThat(crawler.getAllResponses())
        .hasSize(5)
        .extracting(HalResponse::getUri)
        .containsExactly("/", "/1", "/2", "/3", "/4");
  }

  @Test
  public void should_respect_default_limit() throws Exception {

    EndlessLoopHalResourceLoader loader = new EndlessLoopHalResourceLoader();

    HalCrawler crawler = new HalCrawler(loader);

    assertThat(crawler.getAllResponses())
        .hasSize(1000)
        .extracting(HalResponse::getUri);
  }

  @Test
  public void should_respect_custom_limit() throws Exception {

    EndlessLoopHalResourceLoader loader = new EndlessLoopHalResourceLoader();

    HalCrawler crawler = new HalCrawler(loader)
        .withLimit(10);

    assertThat(crawler.getAllResponses())
        .hasSize(10)
        .extracting(HalResponse::getUri)
        .containsExactly("/", "/1", "/2", "/3", "/4", "/5", "/6", "/7", "/8", "/9");
  }

  @Test
  public void should_not_crawl_multiple_times() throws Exception {

    EndlessLoopHalResourceLoader loader = new EndlessLoopHalResourceLoader()
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
  public void should_modify_uris() throws Exception {

    EndlessLoopHalResourceLoader loader = new EndlessLoopHalResourceLoader()
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

  class EndlessLoopHalResourceLoader implements HalResourceLoader {

    private Integer limit;

    private boolean linkTemplate;

    @Override
    public Single<HalResponse> getHalResource(String uri) {

      int nextIndex = getNextIndex(uri);

      HalResource hal = new HalResource(uri);
      if (limit == null || nextIndex < limit) {
        hal.addLinks(StandardRelations.NEXT, new Link("/" + nextIndex));
      }
      if (linkTemplate) {
        hal.addLinks("foo:template", new Link("/{index}"));
        hal.addLinks("curies", new Link("/docs/foo").setName("foo"));
      }

      return Single.just(new HalResponse()
          .withStatus(200)
          .withUri(uri)
          .withBody(hal));
    }

    public EndlessLoopHalResourceLoader withLinkTemplate() {
      linkTemplate = true;
      return this;
    }

    EndlessLoopHalResourceLoader withLimit(int limit) {
      this.limit = limit;
      return this;
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
