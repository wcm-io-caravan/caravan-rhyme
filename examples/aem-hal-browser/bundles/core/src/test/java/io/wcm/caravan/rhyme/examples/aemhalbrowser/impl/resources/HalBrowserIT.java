package io.wcm.caravan.rhyme.examples.aemhalbrowser.impl.resources;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.impl.HalApiServlet;
import io.wcm.caravan.rhyme.aem.impl.RhymeResourceRegistry;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
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
public class HalBrowserIT {

  private AemContext context = AppAemContext.newAemContext();

  private AemRepository getRepository() {

    String entryPointUrl = getEntryPointUrlThroughRegistration();

    assertThat(entryPointUrl)
        .isEqualTo("/.aemrepository.rhyme");

    HalApiServlet servlet = context.getService(HalApiServlet.class);

    HalApiClient halApiClient = ServletIntegrationTestSupport.createHalApiClient(servlet, context.resourceResolver());

    return halApiClient.getRemoteResource(entryPointUrl, AemRepository.class);
  }

  @Test
  void should_allow_browsing_to_scaled_rendition_of_asset_linked_from_page() {

    String pagePath = "/content/foo";
    context.create().page(pagePath);

    String assetPath = "/content/asset";
    context.create().asset(assetPath, "/caravan.png", "image/png");

    context.create().resource(pagePath + "/jcr:content/component", ImmutableMap.of("mediaRef", assetPath));

    SlingResource resource = getRepository().getResource(pagePath).get();

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

    AemRendition rendition = linkedAssets.get(0).getRendition(50, 50).get();

    assertThat(rendition.getHeight())
        .isEqualTo(50);

    assertThat(rendition.getWidth())
        .isEqualTo(50);

    assertThat(rendition.getInvalidReason())
        .isEmpty();
  }

  private String getEntryPointUrlThroughRegistration() {

    context.request().setResource(context.resourceResolver().getResource("/"));

    SlingRhyme rhyme = context.request().adaptTo(SlingRhyme.class);

    SlingResourceAdapter adapter = rhyme.adaptTo(SlingResourceAdapter.class);

    RhymeResourceRegistry registry = context.getService(RhymeResourceRegistry.class);

    LinkableResource entryPoint = registry.getAllApiEntryPoints(adapter)
        .findFirst()
        .get();

    return entryPoint.createLink().getHref();
  }
}
