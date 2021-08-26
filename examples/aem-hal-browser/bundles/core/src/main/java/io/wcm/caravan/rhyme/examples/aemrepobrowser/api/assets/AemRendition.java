package io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets;

import java.util.Optional;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.AemRelations;

@HalApiInterface
public interface AemRendition extends LinkableResource {

  @ResourceState
  AemRenditionProperties getProperties();

  @Related(AemRelations.BINARY)
  Optional<LinkableResource> getBinaryResource();

  @Related(AemRelations.ASSET)
  AemAsset getAsset();
}
