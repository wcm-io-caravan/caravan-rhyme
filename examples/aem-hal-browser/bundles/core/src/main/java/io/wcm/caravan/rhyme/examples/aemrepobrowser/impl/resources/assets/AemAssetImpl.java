package io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.assets;

import java.util.Optional;

import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.dam.api.Asset;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemAsset;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemRendition;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.SlingResource;
import io.wcm.handler.media.MediaHandler;

@Model(adaptables = SlingRhyme.class, adapters = AemAsset.class)
public class AemAssetImpl extends AbstractLinkableResource implements AemAsset {

  @Self
  private Asset asset;

  @Self
  private MediaHandler mediaHandler;

  @Override
  public String getName() {
    return asset.getName();
  }

  @Override
  public String getMimeType() {
    return asset.getMimeType();
  }

  @Override
  public SlingResource asSlingResource() {

    return resourceAdapter
        .selectCurrentResource()
        .adaptTo(SlingResource.class)
        .getInstance();
  }

  @Override
  public Optional<Link> getOriginalRendition() {

    return Optional.of(new BinaryAssetResource(mediaHandler, asset)
        .withTitle("The binary data of this asset's original rendition")
        .createLink());
  }

  @Override
  public Optional<AemRendition> getRendition(Integer width, Integer height) {

    return resourceAdapter
        .selectCurrentResource()
        .adaptTo(AemRendition.class, AemRenditionImpl.class)
        .withLinkTitle("Get a dynamic rendition for this asset with the specified width and/or height")
        .withModifications(impl -> impl.setWidthAndHeight(width, height))
        .withPartialLinkTemplate()
        .getOptional();
  }

  @Override
  protected String getDefaultLinkTitle() {
    return "AEM asset with MIME-type " + asset.getMimeType() + " and " + asset.getRenditions().size() + " renditions";
  }


}
