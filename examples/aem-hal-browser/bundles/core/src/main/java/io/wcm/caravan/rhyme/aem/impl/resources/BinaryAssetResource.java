package io.wcm.caravan.rhyme.aem.impl.resources;

import org.apache.sling.api.resource.Resource;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.integration.impl.ResourceUtils;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.media.Media;
import io.wcm.handler.media.MediaBuilder;
import io.wcm.handler.media.MediaHandler;

class BinaryAssetResource implements LinkableResource {

  private final MediaHandler mediaHandler;
  private final Resource assetResource;
  private final Asset asset;
  private final Rendition rendition;

  private String title;

  public BinaryAssetResource(MediaHandler mediaHandler, Asset asset) {
    this.mediaHandler = mediaHandler;
    this.assetResource = asset.adaptTo(Resource.class);
    this.asset = asset;
    this.rendition = asset.getOriginal();
  }

  public BinaryAssetResource(MediaHandler mediaHandler, Rendition rendition) {
    this.mediaHandler = mediaHandler;
    this.assetResource = ResourceUtils.getParentAssetResource(rendition);
    this.asset = rendition.getAsset();
    this.rendition = rendition;
  }

  @Override
  public Link createLink() {

    MediaBuilder mediaBuilder = mediaHandler.get(assetResource.getPath());
    Media media = mediaBuilder.build();

    String mediaUrl = mediaBuilder.buildUrl();

    return new Link(mediaUrl)
        .setType(asset.getMimeType())
        .setTitle(title);
  }

  public BinaryAssetResource withTitle(String value) {
    title = value;
    return this;
  }
}
