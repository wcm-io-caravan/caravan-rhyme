package io.wcm.caravan.rhyme.aem.impl.resources.assets;

import org.apache.sling.api.resource.Resource;

import com.day.cq.dam.api.Asset;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.media.MediaBuilder;
import io.wcm.handler.media.MediaHandler;

class BinaryAssetResource implements LinkableResource {


  private final MediaBuilder mediaBuilder;

  private String title;

  public BinaryAssetResource(MediaHandler mediaHandler, Asset asset) {

    Resource assetResource = asset.adaptTo(Resource.class);
    this.mediaBuilder = mediaHandler.get(assetResource.getPath());
  }

  public BinaryAssetResource(MediaBuilder builder) {
    this.mediaBuilder = builder;
  }

  @Override
  public Link createLink() {

    String mediaUrl = mediaBuilder.buildUrl();

    return new Link(mediaUrl)
        .setTitle(title);
  }

  public BinaryAssetResource withTitle(String value) {
    title = value;
    return this;
  }
}
