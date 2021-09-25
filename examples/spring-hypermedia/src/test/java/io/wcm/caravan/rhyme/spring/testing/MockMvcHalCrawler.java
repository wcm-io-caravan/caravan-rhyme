package io.wcm.caravan.rhyme.spring.testing;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;

@Component
public class MockMvcHalCrawler {

  private static final Logger log = LoggerFactory.getLogger(MockMvcHalCrawler.class);

  @Autowired
  private MockMvcHalResourceLoader mockMvcResourceLoader;

  public List<HalResource> getAllResources(String entryPointPath) {

    CrawlerImpl crawler = new CrawlerImpl(entryPointPath, 1000);

    crawler.startCrawling();

    return crawler.fetchedResources.values().stream()
        .collect(Collectors.toList());
  }

  private class CrawlerImpl {

    private final Deque<String> urlsToFetch = new LinkedList<>();

    private final Map<String, HalResource> fetchedResources = new LinkedHashMap<>();

    private final int limit;

    private CrawlerImpl(String initialUrl, int limit) {
      urlsToFetch.add(initialUrl);
      this.limit = limit;
    }

    void startCrawling() {

      while (!urlsToFetch.isEmpty()) {

        String nextUrl = urlsToFetch.pop();

        fetchResourceAndExtractResolvedLinks(nextUrl)
            .filter(linkedUrl -> !fetchedResources.containsKey(linkedUrl))
            .forEach(urlsToFetch::add);

        if (fetchedResources.size() >= limit) {
          log.warn("The limit of {} resources to crawl has been reached", limit);
          break;
        }
      }
    }

    Stream<String> fetchResourceAndExtractResolvedLinks(String url) {

      HalResponse response = mockMvcResourceLoader.getHalResource(url).blockingGet();

      fetchedResources.put(url, response.getBody());

      return collectResolvedLinks(response.getBody())
          .map(Link::getHref)
          .distinct();
    }

    Stream<Link> collectResolvedLinks(HalResource hal) {

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
}
