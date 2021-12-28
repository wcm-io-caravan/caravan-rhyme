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
package io.wcm.caravan.rhyme.spring.testing;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

/**
 * A crawler that recursively loads all resources that are linked from the services entry point.
 */
public class HalCrawler {

  private static final Logger log = LoggerFactory.getLogger(HalCrawler.class);

  private final HalResourceLoader resourceLoader;

  private final Deque<String> urlsLeftToCrawl = new LinkedList<>();

  private final Map<String, HalResponse> crawledUrlsAndResponses = new LinkedHashMap<>();

  private int limit = 1000;

  private Function<String, String> urlModifier = Function.identity();

  /**
   * @param resourceLoader used to to load the resources
   */
  public HalCrawler(HalResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  /**
   * Use a different entry point than "/" to start crawling
   * @param url the first URL to load and look for links
   * @return this
   */
  public HalCrawler withEntryPoint(String url) {

    addUrlUnlessAlreadyProcessed(url);
    return this;
  }

  /**
   * Stop crawling after a specific number of resources has been retrieved.
   * @param maxNumResources how many resources to load
   * @return this
   */
  public HalCrawler withLimit(int maxNumResources) {

    limit = maxNumResources;
    return this;
  }

  /**
   * @param function to be applied to every URI before it will be crawled
   * @return this
   */
  public HalCrawler withModifiedUrls(Function<String, String> function) {

    urlModifier = function;
    return this;
  }

  /**
   * Start crawling and return all responses
   * @return a list of all {@link HalResponse}s in the order they have been retrieved
   */
  public List<HalResponse> getAllResponses() {

    crawlAllResourcesOnce();

    return new ArrayList<>(crawledUrlsAndResponses.values());
  }

  private void crawlAllResourcesOnce() {

    if (crawledUrlsAndResponses.isEmpty()) {
      crawlResourcesRecursively();
    }
  }

  private void addUrlUnlessAlreadyProcessed(String url) {

    String urlToCrawl = urlModifier.apply(url);

    if (!crawledUrlsAndResponses.containsKey(urlToCrawl)) {
      urlsLeftToCrawl.add(urlToCrawl);
    }
  }

  private void crawlResourcesRecursively() {

    if (urlsLeftToCrawl.isEmpty()) {
      addUrlUnlessAlreadyProcessed("/");
    }

    while (!urlsLeftToCrawl.isEmpty()) {

      String nextUrl = urlsLeftToCrawl.pop();

      fetchResourceAndExtractResolvedLinks(nextUrl)
          .forEach(this::addUrlUnlessAlreadyProcessed);

      if (crawledUrlsAndResponses.size() >= limit) {
        log.warn("The limit of {} resources to crawl has been reached", limit);
        break;
      }
    }
  }

  private Stream<String> fetchResourceAndExtractResolvedLinks(String url) {

    HalResponse response = resourceLoader.getHalResource(url).blockingGet();

    crawledUrlsAndResponses.put(url, response);

    return collectResolvedLinks(response.getBody())
        .map(Link::getHref)
        .distinct();
  }

  private Stream<Link> collectResolvedLinks(HalResource hal) {

    Stream<Link> directlyLinked = hal.getLinks().entries().stream()
        .filter(entry -> !"curies".equals(entry.getKey()))
        .map(Entry::getValue)
        .filter(link -> !link.isTemplated());

    Stream<Link> linkedFromEmbedded = hal.getEmbedded().values().stream()
        .flatMap(this::collectResolvedLinks);

    return Stream.concat(directlyLinked, linkedFromEmbedded)
        .distinct();
  }
}
