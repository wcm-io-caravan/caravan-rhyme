package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import static io.wcm.caravan.rhyme.examples.spring.hypermedia.CompanyApiStickyParameters.USE_EMBEDDED_RESOURCES;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.testing.client.HalCrawler;

/**
 * A variation of the {@link MockMvcClientIT} test that verifies
 * if the {@link CompanyApi} functionality remains the same when no embedded resources are used.
 */
public class MockMvcClientWithoutEmbeddedIT extends MockMvcClientIT {

  private final CompanyApiSettings dontUseEmbedded = new CompanyApiSettings() {

    @Override
    public Boolean getUseEmbeddedResources() {
      return false;
    }
  };

  @Override
  protected CompanyApi getApiImplementionOrClientProxy() {

    // since we are extending AbstractCompanyIT, all tests defined there will be executed
    // against the client proxy that is created here

    return super.getApiImplementionOrClientProxy()
        // and we are returning the alternative entry point with the settings
        // that disable the usage of embedded resource
        .withSettings(dontUseEmbedded);
  }

  @Test
  public void entry_point_url_should_contain_sticky_parameter() {

    CompanyApi resource = getApiImplementionOrClientProxy();

    URI uri = getURI(resource);

    assertThat(uri)
        .hasParameter(USE_EMBEDDED_RESOURCES, "false");
  }

  @Test
  public void no_resource_should_contain_embedded_resources() {

    List<HalResponse> allResponses = crawlAllResponses();

    for (HalResponse response : allResponses) {

      Set<String> relationsOfEmbedded = response.getBody().getEmbedded().keySet();

      assertThat(relationsOfEmbedded)
          .as("embedded relations in resource at " + response.getUri())
          .isEmpty();
    }
  }

  @Test
  public void all_links_in_every_resource_should_contain_sticky_parameter() {

    List<HalResponse> allResponses = crawlAllResponses();

    for (HalResponse response : allResponses) {

      assertThat(response.getBody().getLinks().entries())
          .filteredOn(this::isLinkThatShouldHaveStickyParameter)
          .extracting(Entry::getValue)
          .extracting(Link::getHref)
          .as("all links in resource at " + response.getUri())
          .allMatch(href -> href.contains(USE_EMBEDDED_RESOURCES + "=false"));
    }
  }

  private boolean isLinkThatShouldHaveStickyParameter(Entry<String, Link> entry) {

    String relation = entry.getKey();

    return !(relation.equals("curies") || relation.equals("company:settings"));
  }

  private List<HalResponse> crawlAllResponses() {

    String entryPointUri = getURI(getApiImplementionOrClientProxy()).toString();

    HalCrawler crawler = new HalCrawler(mockMvcResourceLoader)
        .withEntryPoint(entryPointUri);

    return crawler.getAllResponses();
  }

  private URI getURI(LinkableResource resource) {

    String href = resource.createLink().getHref();
    return URI.create(href);
  }
}
