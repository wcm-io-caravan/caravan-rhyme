package io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.assets;

import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.dam.api.Asset;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemAsset;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemRendition;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemRenditionProperties;
import io.wcm.handler.media.Media;
import io.wcm.handler.media.MediaArgs;
import io.wcm.handler.media.MediaBuilder;
import io.wcm.handler.media.MediaHandler;
import io.wcm.handler.media.Rendition;
import io.wcm.handler.media.format.MediaFormat;
import io.wcm.handler.media.format.MediaFormatBuilder;

@Model(adaptables = SlingRhyme.class, adapters = AemRendition.class)
public class AemRenditionImpl extends AbstractLinkableResource implements AemRendition {

  public static final String HEIGHT = "height";
  public static final String WIDTH = "width";

  @Self
  private Asset asset;

  @Self
  private MediaHandler mediaHandler;

  @Self
  private Resource resource;

  @Self
  private SlingHttpServletRequest request;

  private Integer width;
  private Integer height;

  private MediaBuilder mediaBuilder;
  private Media media;

  @PostConstruct
  void activate() {
    setWidthAndHeight(parseRequestParameter(WIDTH), parseRequestParameter(HEIGHT));

    this.mediaBuilder = createMediaBuilder();
    this.media = mediaBuilder.build();
  }

  public void setWidthAndHeight(Integer width, Integer height) {
    this.width = width;
    this.height = height;

    Map<String, Object> queryParameters = getLinkProperties().getQueryParameters();
    queryParameters.clear();
    queryParameters.put(WIDTH, width);
    queryParameters.put(HEIGHT, height);
  }

  private Integer parseRequestParameter(String name) {
    RequestParameter param = request.getRequestParameter(name);
    if (param == null) {
      return null;
    }
    try {
      return Integer.parseInt(param.getString());
    }
    catch (NumberFormatException ex) {
      return null;
    }
  }

  private MediaBuilder createMediaBuilder() {
    MediaFormatBuilder formatBuilder = MediaFormatBuilder.create("foo")
        .extensions("gif", "jpg", "png");

    if (width != null) {
      formatBuilder = formatBuilder.width(width);
    }
    if (height != null) {
      formatBuilder = formatBuilder.height(height);
    }
    MediaFormat format = formatBuilder.build();

    return mediaHandler.get(resource.getPath())
        .args(new MediaArgs(format).autoCrop(true));
  }


  @Override
  public AemRenditionProperties getProperties() {

    Rendition rendition = media.getRendition();

    return new AemRenditionProperties() {

      @Override
      public Integer getWidth() {
        if (rendition == null) {
          return null;
        }
        return (int)rendition.getWidth();
      }

      @Override
      public Integer getHeight() {
        if (rendition == null) {
          return null;
        }
        return (int)rendition.getHeight();
      }

      @Override
      public String getMimeType() {
        if (rendition == null) {
          return null;
        }
        return rendition.getMimeType();
      }

      @Override
      public boolean isValid() {
        return media.isValid();
      }

      @Override
      public String getInvalidReason() {
        if (media.isValid()) {
          return null;
        }
        return media.getMediaInvalidReason().toString();
      }

    };
  }

  @Override
  public Optional<LinkableResource> getBinaryResource() {

    if (!media.isValid() || media.getRendition() == null) {
      return Optional.empty();
    }

    return Optional.of(new BinaryAssetResource(mediaBuilder)
        .withTitle("Download the dynamic rendition " + media.getRendition()));
  }

  @Override
  public AemAsset getAsset() {

    return resourceAdapter
        .selectCurrentResource()
        .adaptTo(AemAsset.class)
        .withLinkTitle("The asset resource for this rendition")
        .getInstance();
  }

  @Override
  protected String getDefaultLinkTitle() {
    return "dynamic rendition of asset at " + asset.getPath() + " with width=" + width + " and height=" + height;
  }


}
