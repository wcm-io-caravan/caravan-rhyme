package io.wcm.caravan.rhyme.examples.aemhalbrowser.impl.resources.assets;

import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.assertResourceCanBeRendered;
import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.createRhymeInstance;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemAsset;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemRendition;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.SlingResource;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class AemAssetImplTest {

  private static final String PATH = "/content/foo";

  private AemContext context = AppAemContext.newAemContext();

  private AemAsset createSlingModel(String path) {

    SlingRhyme slingRhyme = createRhymeInstance(context, path);

    AemAsset asset = slingRhyme.adaptTo(AemAsset.class);

    assertThat(asset)
        .isNotNull();

    return asset;
  }

  @Test
  void can_render_minimal_asset_and_return_name_and_mime_type() {

    String mimeType = "text/png";

    context.create().asset(PATH, 100, 100, mimeType);

    AemAsset asset = createSlingModel(PATH);

    assertResourceCanBeRendered(asset);

    assertThat(asset.getName())
        .isEqualTo("foo");

    assertThat(asset.getMimeType())
        .isEqualTo(mimeType);
  }

  @Test
  void asSlingResource_returns_link_to_generic_sling_resource() {

    context.create().asset(PATH, 100, 100, "text/png");

    SlingResource slingModel = createSlingModel(PATH).asSlingResource();

    assertThat(slingModel.createLink().getHref())
        .isEqualTo("/content/foo.slingresource.rhyme");
  }


  @Test
  void getOriginalRendition_returns_link_to_binary_rendition() {

    String mimeType = "text/png";

    context.create().asset(PATH, 100, 100, mimeType);

    Link binaryLink = createSlingModel(PATH).getOriginalRendition().get();

    assertThat(binaryLink.getHref())
        .isEqualTo("/content/foo/_jcr_content/renditions/original./foo");

    assertThat(binaryLink.getType())
        .isEqualTo(mimeType);

    assertThat(binaryLink.getTitle())
        .isEqualTo("The binary data of this asset's original rendition");
  }

  @Test
  void getOriginalRendition_returns_empty_if_no_original_rendition_present() {

    context.create().contentFragmentStructured(PATH, Collections.emptyMap());

    Optional<Link> binaryLink = createSlingModel(PATH).getOriginalRendition();

    assertThat(binaryLink)
        .isNotPresent();
  }

  @Test
  void getRendition_returns_link_template_if_null_values_for_dimensions_are_given() {

    String mimeType = "text/png";

    context.create().asset(PATH, 100, 100, mimeType);

    AemRendition rendition = createSlingModel(PATH).getRendition(null, null).get();

    assertThat(rendition.createLink().getHref())
        .isEqualTo("/content/foo.aemrendition.rhyme{?width,height}");
  }

  @Test
  void getRendition_returns_resolved_rendition_link_if_values_for_dimensions_are_given() {

    String mimeType = "text/png";

    context.create().asset(PATH, 100, 100, mimeType);

    AemRendition rendition = createSlingModel(PATH).getRendition(50, 50).get();

    assertThat(rendition.getWidth())
        .isEqualTo(50);

    assertThat(rendition.getHeight())
        .isEqualTo(50);

    assertThat(rendition.createLink().getHref())
        .isEqualTo("/content/foo.aemrendition.rhyme?width=50&height=50");
  }

  @Test
  void createLink_sets_names_and_title() {

    String mimeType = "text/png";

    context.create().asset(PATH, 100, 100, mimeType);

    Link selfLink = createSlingModel(PATH).createLink();

    assertThat(selfLink.getTitle())
        .startsWith("AEM asset with MIME-type " + mimeType);

    assertThat(selfLink.getName())
        .isEqualTo("foo");
  }
}
