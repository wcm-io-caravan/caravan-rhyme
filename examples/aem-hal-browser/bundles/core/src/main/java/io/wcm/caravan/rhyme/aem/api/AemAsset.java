package io.wcm.caravan.rhyme.aem.api;

import java.util.Optional;
import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface AemAsset extends LinkableResource {

  @ResourceState
  AemAssetProperties getProperties();

  @Related(AemRelations.SLING_RESOURCE)
  SlingResource asSlingResource();

  @Related(AemRelations.RENDITION)
  Stream<AemRendition> getRenditions();

  @Related(AemRelations.BINARY)
  Optional<LinkableResource> getOriginalRendition();
}
