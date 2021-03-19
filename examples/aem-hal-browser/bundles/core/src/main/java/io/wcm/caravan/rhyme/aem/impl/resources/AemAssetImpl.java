package io.wcm.caravan.rhyme.aem.impl.resources;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.dam.api.Asset;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.AemAsset;
import io.wcm.caravan.rhyme.aem.api.AemAssetProperties;
import io.wcm.caravan.rhyme.aem.api.AemRendition;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.handler.media.MediaHandler;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, AemAsset.class }, resourceType = "wcm/foundation/components/basicpage/v1/basicpage")

public class AemAssetImpl implements AemAsset {

  public static final String SELECTOR = "aemasset";

  @RhymeObject
  private SlingResourceAdapter resourceAdapter;

  @RhymeObject
  private SlingLinkBuilder linkBuilder;

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

    return resourceAdapter.getSelfAs(SlingResource.class).get();
  }

  @Override
  public Optional<LinkableResource> getOriginalRendition() {

    return Optional.of(new BinaryAssetResource(mediaHandler, asset));
  }

  @Override
  public Stream<AemRendition> getRenditions() {

    return asset.getRenditions().stream()
        .map(rendition -> resourceAdapter.withDifferentResource(rendition.getPath()))
        .map(adapter -> adapter.getSelfAs(AemRendition.class).get());
  }

  @Override
  public Link createLink() {

    return linkBuilder.createLinkToCurrentResource(this)
        .setTitle("AEM asset with MIME-type " + asset.getMimeType() + " and " + asset.getRenditions().size() + " renditions");
  }


}
