package io.wcm.caravan.rhyme.aem.api;

import java.util.Optional;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface AemRendition extends LinkableResource {

  @ResourceState
  AemRenditionProperties getProperties();

  @Related(AemRelations.BINARY)
  Optional<LinkableResource> getBinaryResource();

  @Related(AemRelations.ASSET)
  AemAsset getAsset();
}
