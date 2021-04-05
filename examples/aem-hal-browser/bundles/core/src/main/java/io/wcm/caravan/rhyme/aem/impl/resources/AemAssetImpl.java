package io.wcm.caravan.rhyme.aem.impl.resources;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.dam.api.Asset;

import io.wcm.caravan.rhyme.aem.api.AemAsset;
import io.wcm.caravan.rhyme.aem.api.AemAssetProperties;
import io.wcm.caravan.rhyme.aem.api.AemRendition;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.AbstractLinkableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.media.MediaHandler;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, AemAsset.class })

public class AemAssetImpl extends AbstractLinkableResource implements AemAsset {

  public static final String SELECTOR = "aemasset";

  @Self
  private Asset asset;

  @Self
  private MediaHandler mediaHandler;

  @Self
  private Resource resource;

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

    return Optional.of(new BinaryAssetResource(mediaHandler, asset));
  }

  @Override
  public Stream<AemRendition> getRenditions() {

    ResourceResolver resolver = resource.getResourceResolver();

    Stream<Resource> renditionResources = asset.getRenditions().stream()
        .map(rendition -> resolver.getResource(rendition.getPath()))
        .filter(Objects::nonNull);

    return resourceAdapter.select(renditionResources)
        .adaptTo(AemRendition.class)
        .getStream();
  }

  @Override
  protected String getDefaultLinkTitle() {
    return "AEM asset with MIME-type " + asset.getMimeType() + " and " + asset.getRenditions().size() + " renditions";
  }


}
