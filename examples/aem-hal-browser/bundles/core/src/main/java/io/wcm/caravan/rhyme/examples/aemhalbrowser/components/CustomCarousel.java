package io.wcm.caravan.rhyme.examples.aemhalbrowser.components;

import static io.wcm.handler.media.MediaNameConstants.PROP_CSS_CLASS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import io.wcm.handler.media.Media;
import io.wcm.handler.media.MediaHandler;

/**
 * Model for the custom carousel component.
 * <p>
 * Please note: There is already is a pre-built "Carousel" Core Component which does basically the same
 * as this component with a much more sophisticated edit mode support. Use it, instead of this demo component!
 * This demo component is only an example for a custom standalone component.
 * </p>
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class CustomCarousel {

  static final String NN_SLIDES = "slides";

  private String id;
  private List<Media> slideImages;

  @Self
  private SlingHttpServletRequest request;
  @Self
  private MediaHandler mediaHandler;

  @PostConstruct
  private void activate() {
    id = buildId();
    slideImages = buildSlideImages();
  }

  private String buildId() {
    // build unique id from component path.
    return "customcarousel-" + request.getResource().getPath().hashCode();
  }

  private List<Media> buildSlideImages() {
    List<Media> images = new ArrayList<>();

    // get configured media references and convert them to image urls
    Resource slides = request.getResource().getChild(NN_SLIDES);
    if (slides != null) {
      for (Resource slide : slides.getChildren()) {
        Media img = mediaHandler.get(slide)
            .property(PROP_CSS_CLASS, "d-block w-100")
            .build();
        if (img.isValid()) {
          images.add(img);
        }
      }
    }

    return images;
  }

  /**
   * @return Unique ID of this component that can be used in HTML markup
   */
  public String getId() {
    return id;
  }

  /**
   * @return List of images for each slide
   */
  public List<Media> getSlideImages() {
    return Collections.unmodifiableList(this.slideImages);
  }

}
