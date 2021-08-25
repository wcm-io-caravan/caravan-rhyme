package io.wcm.caravan.rhyme.aem.impl.resources.assets;

import java.util.Optional;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.dam.api.Asset;

import io.wcm.caravan.rhyme.aem.api.assets.AemAsset;
import io.wcm.caravan.rhyme.aem.api.assets.AemAssetProperties;
import io.wcm.caravan.rhyme.aem.api.assets.AemRendition;
import io.wcm.caravan.rhyme.aem.api.generic.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.AbstractLinkableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.media.MediaHandler;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, AemAsset.class })
public class AemAssetImpl extends AbstractLinkableResource implements AemAsset {

  @Self
  private Asset asset;

  @Self
  private MediaHandler mediaHandler;

  @Override
  public AemAssetProperties getProperties() {

    return new AemAssetProperties() {

      @Override
      public String getName() {
        return asset.getName();
      }

      @Override
      public String getMimeType() {
        return asset.getMimeType();
      }

    };
  }

  @Override
  public SlingResource asSlingResource() {

    return resourceAdapter
        .selectCurrentResource()
        .adaptTo(SlingResource.class)
        .getInstance();
  }

  @Override
  public Optional<LinkableResource> getOriginalRendition() {

    return Optional.of(new BinaryAssetResource(mediaHandler, asset)
        .withTitle("The binary data of this asset's original rendition"));
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
