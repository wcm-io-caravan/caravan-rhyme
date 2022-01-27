package io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.assets;

import org.apache.sling.api.resource.Resource;

import com.day.cq.dam.api.Asset;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.media.MediaBuilder;
import io.wcm.handler.media.MediaHandler;
import io.wcm.handler.media.Rendition;

class BinaryAssetResource implements LinkableResource {


  private final MediaBuilder mediaBuilder;

  private final String mimeType;

  private String title;

  public BinaryAssetResource(MediaHandler mediaHandler, Asset asset) {

    Resource assetResource = asset.adaptTo(Resource.class);
    this.mediaBuilder = mediaHandler.get(assetResource.getPath());
    this.mimeType = asset.getMimeType();
  }

  public BinaryAssetResource(MediaBuilder builder, Rendition rendition) {
    this.mediaBuilder = builder;
    this.mimeType = rendition.getMimeType();
  }

  @Override
  public Link createLink() {

    String mediaUrl = mediaBuilder.buildUrl();

    return new Link(mediaUrl)
        .setType(mimeType)
        .setTitle(title);
  }

  public BinaryAssetResource withTitle(String value) {
    title = value;
    return this;
  }
}
