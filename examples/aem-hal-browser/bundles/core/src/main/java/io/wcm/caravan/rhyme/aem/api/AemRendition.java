package io.wcm.caravan.rhyme.aem.api;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface AemRendition extends EmbeddableResource {

  @ResourceState
  AemRenditionProperties getProperties();

  @Related(AemRelations.BINARY)
  LinkableResource getBinaryResource();
}
