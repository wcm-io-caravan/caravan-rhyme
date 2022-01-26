package io.wcm.caravan.rhyme.examples.aemhalbrowser.impl.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.dam.api.Asset;
import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.ServletIntegrationTestSupport;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.AemRepository;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemAsset;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemRendition;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.SlingResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemPage;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class HalBrowserIT {

  private static final String ENTRY_POINT_URL = "/.aemrepository.rhyme";
  private AemContext context = AppAemContext.newAemContext();

  @Test
  void entry_point_should_be_registered() {

    String entryPointUrl = ServletIntegrationTestSupport.getFirstRegisteredEntryPointUrl(context);

    assertThat(entryPointUrl)
        .isEqualTo(ENTRY_POINT_URL);
  }

  @Test
  void should_allow_browsing_to_scaled_rendition_of_asset_linked_from_page() {

    // set up a page that has a reference to an asset

    String assetPath = "/content/asset";
    Asset asset = context.create().asset(assetPath, "/caravan.png", "image/png");
    context.create().assetRenditionWebEnabled(asset);

    String pagePath = "/content/foo";
    context.create().page(pagePath);
    context.create().resource(pagePath + "/jcr:content/component", ImmutableMap.of("mediaRef", assetPath));

    // now use only client proxies to navigate through the resources

    AemRepository repository = ServletIntegrationTestSupport.createEntryPointProxy(AemRepository.class, context);

    SlingResource resource = repository.getResource(pagePath).get();

    assertThat(resource.getProperties())
        .isNotEmpty();

    AemPage aemPage = resource.asAemPage().get();

    assertThat(aemPage.getTitle())
        .isEqualTo("foo");

    List<AemAsset> linkedAssets = aemPage.getLinkedContent().getLinkedAssets().collect(Collectors.toList());

    assertThat(linkedAssets)
        .hasSize(1)
        .first()
        .extracting(AemAsset::getName)
        .isEqualTo("asset");

    // TODO: find out why auto cropping to a different aspect ratio doesn't work during integration test
    int scaledWidth = 250;
    int scaledHeight = 38;

    AemRendition rendition = linkedAssets.get(0).getRendition(scaledWidth, scaledHeight).get();

    assertThat(rendition.getWidth())
        .isEqualTo(scaledWidth);

    assertThat(rendition.getHeight())
        .isEqualTo(scaledHeight);

    assertThat(rendition.getInvalidReason())
        .isEmpty();

    assertThat(rendition.getBinaryResource())
        .isPresent()
        .get()
        .extracting(Link::getHref)
        .isEqualTo("/content/asset/_jcr_content/renditions/cq5dam.web.1280.1280.jpg.image_file.250.38.file/cq5dam.web.1280.1280.jpg");
  }

}
