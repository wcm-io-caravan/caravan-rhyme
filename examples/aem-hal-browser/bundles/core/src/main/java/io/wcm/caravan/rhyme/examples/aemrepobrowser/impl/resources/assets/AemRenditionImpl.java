package io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.assets;

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.dam.api.Asset;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.parameters.QueryParam;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemAsset;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemRendition;
import io.wcm.handler.media.Media;
import io.wcm.handler.media.MediaArgs;
import io.wcm.handler.media.MediaBuilder;
import io.wcm.handler.media.MediaHandler;
import io.wcm.handler.media.Rendition;
import io.wcm.handler.media.format.MediaFormat;
import io.wcm.handler.media.format.MediaFormatBuilder;

@Model(adaptables = SlingRhyme.class, adapters = AemRendition.class)
public class AemRenditionImpl extends AbstractLinkableResource implements AemRendition {

  @Self
  private Asset asset;

  @Self
  private MediaHandler mediaHandler;

  @Self
  private Resource resource;

  @QueryParam
  private Integer width;

  @QueryParam
  private Integer height;

  private MediaBuilder mediaBuilder;
  private Media media;
  private Rendition rendition;

  @PostConstruct
  void activate() {
    this.mediaBuilder = createMediaBuilder();
    this.media = mediaBuilder.build();
    this.rendition = media.getRendition();
  }

  public void setWidthAndHeight(Integer width, Integer height) {
    this.width = width;
    this.height = height;
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
  public Integer getWidth() {
    if (width == null) {
      throw new HalApiServerException(HttpStatus.SC_BAD_REQUEST, "the width parameter is required");
    }
    return width;
  }

  @Override
  public Integer getHeight() {
    if (height == null) {
      throw new HalApiServerException(HttpStatus.SC_BAD_REQUEST, "the width parameter is required");
    }
    return height;
  }

  @Override
  public Optional<String> getMimeType() {
    if (rendition == null) {
      return Optional.ofNullable(asset.getMimeType());
    }
    return Optional.of(rendition.getMimeType());
  }

  @Override
  public boolean isValid() {
    return media.isValid();
  }

  @Override
  public Optional<String> getInvalidReason() {
    if (media.isValid()) {
      return Optional.empty();
    }
    return Optional.of(media.getMediaInvalidReason().toString());
  }

  @Override
  public Optional<Link> getBinaryResource() {

    if (!media.isValid() || media.getRendition() == null) {
      return Optional.empty();
    }

    return Optional.of(new BinaryAssetResource(mediaBuilder, media.getRendition())
        .withTitle("Download the dynamic rendition " + media.getRendition())
        .createLink());
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
