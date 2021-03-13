package io.wcm.caravan.rhyme.examples.aemhalbrowser.components;

import static io.wcm.handler.media.MediaNameConstants.PN_MEDIA_REF_STANDARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.wcm.api.Page;
import com.google.common.collect.ImmutableList;

import io.wcm.handler.media.Media;
import io.wcm.sling.commons.adapter.AdaptTo;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static io.wcm.caravan.rhyme.examples.aemhalbrowser.components.CustomCarousel.NN_SLIDES;
import io.wcm.caravan.rhyme.examples.aemhalbrowser.testcontext.AppAemContext;

@ExtendWith(AemContextExtension.class)
class CustomCarouselTest {

  private final AemContext context = AppAemContext.newAemContext();

  private Page page;
  private Resource resource;

  @BeforeEach
  void setUp() {
    page = context.create().page("/content/mypage");
    resource = context.create().resource(page.getContentResource().getPath() + "/myresource");
    context.currentResource(resource);
  }

  @Test
  void testId() {
    CustomCarousel underTest = AdaptTo.notNull(context.request(), CustomCarousel.class);
    assertNotNull(underTest.getId());
  }

  @Test
  void testSlideImageUrls() {
    context.create().asset("/content/dam/slides/slide1.png", 1200, 450, "image/png");
    context.create().asset("/content/dam/slides/slide2.png", 1200, 450, "image/png");

    context.build().resource(resource.getPath() + "/" + NN_SLIDES)
        .siblingsMode()
        .resource("item1", PN_MEDIA_REF_STANDARD, "/content/dam/slides/slide1.png")
        .resource("item2", PN_MEDIA_REF_STANDARD, "/content/dam/slides/slide2.png");

    CustomCarousel underTest = AdaptTo.notNull(context.request(), CustomCarousel.class);
    assertEquals(ImmutableList.of(
        "/content/dam/slides/slide1.png/_jcr_content/renditions/original./slide1.png",
        "/content/dam/slides/slide2.png/_jcr_content/renditions/original./slide2.png"),
        underTest.getSlideImages().stream()
            .map(Media::getUrl)
            .collect(Collectors.toList()));
  }

  @Test
  void testEmptySlideImageUrls() {
    CustomCarousel underTest = AdaptTo.notNull(context.request(), CustomCarousel.class);
    assertTrue(underTest.getSlideImages().isEmpty());
  }

}
