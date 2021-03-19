package io.wcm.caravan.rhyme.aem.api;

import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;

@HalApiInterface
public interface AemLinkedContent extends EmbeddableResource {

  @Related(AemRelations.PAGE)
  Stream<AemPage> getLinkedPages();

  @Related(AemRelations.ASSET)
  Stream<AemAsset> getLinkedAssets();

  @Related(AemRelations.SLING_RESOURCE)
  Stream<SlingResource> getOtherLinkedResources();
}
