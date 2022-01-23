package io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.sling.api.resource.Resource;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.testing.mock.aem.junit5.AemContext;

public final class SlingRhymeTestUtils {

  private SlingRhymeTestUtils() {
    // static methods only
  }

  public static SlingRhyme createRhymeInstance(AemContext context, String resourcePathFromRequest) {

    Resource resource = context.resourceResolver().getResource(resourcePathFromRequest);

    if (resource == null) {
      resource = context.create().resource(resourcePathFromRequest);
    }

    context.currentResource(resource);
    context.request().setResource(resource);

    return context.request().adaptTo(SlingRhyme.class);
  }

  public static void assertLinkHasHref(String url, Optional<? extends LinkableResource> linkableResource) {
  
    assertThat(linkableResource)
        .isPresent()
        .get()
        .extracting(resource -> resource.createLink().getHref())
        .isEqualTo(url);
  }

  public static void assertResourceCanBeRendered(Object resource) {
  
    assertThat(resource)
        .isInstanceOf(LinkableResource.class);
  
    HalResponse response = RhymeBuilder.create()
        .buildForRequestTo("/")
        .renderResponse((LinkableResource)resource)
        .blockingGet();
  
    assertThat(response.getStatus())
        .isEqualTo(200);
  }

}
