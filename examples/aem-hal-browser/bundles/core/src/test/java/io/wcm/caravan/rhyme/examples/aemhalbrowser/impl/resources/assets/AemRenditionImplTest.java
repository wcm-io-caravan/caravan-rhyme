package io.wcm.caravan.rhyme.examples.aemhalbrowser.impl.resources.assets;

import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.assertRenderingFailsWithStatus;
import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.assertResourceCanBeRendered;
import static io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.SlingRhymeTestUtils.createRhymeInstance;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.dam.api.Asset;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.assets.AemRenditionImpl;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class AemRenditionImplTest {

  private static final String PATH = "/content/foo";

  private AemContext context = AppAemContext.newAemContext();


  private AemRenditionImpl createSlingModel(String path, Integer width, Integer height) {

    String query = "";
    if (width != null) {
      query += "width=" + width + "&";
    }
    if (height != null) {
      query += "height=" + height + "&";
    }
    context.request().setQueryString(query);

    SlingRhyme slingRhyme = createRhymeInstance(context, path);

    AemRenditionImpl rendition = slingRhyme.adaptTo(AemRenditionImpl.class);

    assertThat(rendition)
        .isNotNull();

    return rendition;
  }

  @Test
  void responds_with_bad_request_if_width_and_height_is_null() {

    context.create().asset(PATH, 100, 100, "image/png");

    AemRenditionImpl rendition = createSlingModel(PATH, null, null);

    assertRenderingFailsWithStatus(HttpStatus.SC_BAD_REQUEST, rendition);
  }

  @Test
  void responds_with_bad_request_if_width_is_not_present() {

    context.create().asset(PATH, 100, 100, "image/png");

    AemRenditionImpl rendition = createSlingModel(PATH, null, 50);

    assertRenderingFailsWithStatus(HttpStatus.SC_BAD_REQUEST, rendition);
  }

  @Test
  void responds_with_bad_request_if_height_is_not_present() {

    context.create().asset(PATH, 100, 100, "image/png");

    AemRenditionImpl rendition = createSlingModel(PATH, 50, null);

    assertRenderingFailsWithStatus(HttpStatus.SC_BAD_REQUEST, rendition);
  }

  @Test
  void returns_unscaled_web_rendition_if_width_and_height_match_asset_dimensions() {

    // TODO: find out why this doesn't return a link to the *original* rendition

    int origWidth = 100;
    int origHeight = 150;
    String mimeType = "image/png";
    Asset asset = context.create().asset(PATH, origWidth, origHeight, mimeType);
    context.create().assetRenditionWebEnabled(asset);

    AemRenditionImpl rendition = createSlingModel(PATH, origWidth, origHeight);

    assertResourceCanBeRendered(rendition);

    assertThat(rendition.getWidth())
        .isEqualTo(origWidth);

    assertThat(rendition.getHeight())
        .isEqualTo(origHeight);

    assertThat(rendition.getMimeType())
        .isPresent()
        .get()
        .isEqualTo("image/jpeg");

    assertThat(rendition.getInvalidReason())
        .isNotPresent();

    assertThat(rendition.getBinaryResource())
        .isPresent()
        .get()
        .extracting(Link::getHref)
        .isEqualTo("/content/foo/_jcr_content/renditions/cq5dam.web.1280.1280.jpg./cq5dam.web.1280.1280.jpg");
  }

  @Test
  void returns_scaled_web_rendition_if_width_and_height_match_aspect_ratio() {

    int origWidth = 100;
    int origHeight = 150;
    String mimeType = "image/png";
    Asset asset = context.create().asset(PATH, origWidth, origHeight, mimeType);
    context.create().assetRenditionWebEnabled(asset);

    AemRenditionImpl rendition = createSlingModel(PATH, origWidth / 2, origHeight / 2);

    assertResourceCanBeRendered(rendition);

    assertThat(rendition.getWidth())
        .isEqualTo(origWidth / 2);

    assertThat(rendition.getHeight())
        .isEqualTo(origHeight / 2);

    assertThat(rendition.getMimeType())
        .isPresent()
        .get()
        .isEqualTo("image/jpeg");

    assertThat(rendition.getInvalidReason())
        .isNotPresent();

    assertThat(rendition.getBinaryResource())
        .isPresent()
        .get()
        .extracting(Link::getHref)
        .isEqualTo("/content/foo/_jcr_content/renditions/cq5dam.web.1280.1280.jpg.image_file.50.75.file/cq5dam.web.1280.1280.jpg");
  }

  @Test
  void returns_no_rendition_if_width_and_height_do_not_match_aspect_ratio() {

    // TODO: when running in AEM, this should actually return a auto-cropped rendition.
    // this needs to be verified and the unit test setup adjusted to match that behavior
    int origWidth = 100;
    int origHeight = 150;
    String mimeType = "image/png";
    Asset asset = context.create().asset(PATH, origWidth, origHeight, mimeType);
    context.create().assetRenditionWebEnabled(asset);

    AemRenditionImpl rendition = createSlingModel(PATH, origWidth / 2, origHeight / 3);

    assertResourceCanBeRendered(rendition);

    assertThat(rendition.getWidth())
        .isEqualTo(origWidth / 2);

    assertThat(rendition.getHeight())
        .isEqualTo(origHeight / 3);

    assertThat(rendition.getMimeType())
        .isPresent()
        .get()
        .isEqualTo(mimeType);

    assertThat(rendition.getInvalidReason())
        .isPresent()
        .get()
        .isEqualTo("NO_MATCHING_RENDITION");

    assertThat(rendition.getBinaryResource())
        .isNotPresent();
  }

  @Test
  void setWidthAndHeight_will_set_query_parameters() {

    context.create().asset(PATH, 100, 100, "image/png");

    AemRenditionImpl rendition = createSlingModel(PATH, null, null);
    rendition.setWidthAndHeight(123, 456);

    assertThat(rendition.createLink().getHref())
        .isEqualTo("/content/foo.aemrendition.rhyme?width=123&height=456");
  }
}
