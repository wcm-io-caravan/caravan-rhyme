package io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets;

import java.util.Optional;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.AemRelations;

@HalApiInterface
public interface AemRendition extends LinkableResource {

  @ResourceProperty
  boolean isValid();

  @ResourceProperty
  Optional<String> getInvalidReason();

  @ResourceProperty
  Optional<String> getMimeType();

  @ResourceProperty
  Integer getHeight();

  @ResourceProperty
  Integer getWidth();

  @Related(AemRelations.BINARY)
  Optional<Link> getBinaryResource();

  @Related(AemRelations.ASSET)
  AemAsset getAsset();
}
