package io.wcm.caravan.rhyme.aem.api.assets;

import java.util.Optional;

import io.wcm.caravan.rhyme.aem.api.AemRelations;
import io.wcm.caravan.rhyme.aem.api.generic.SlingResource;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface AemAsset extends LinkableResource {

  @ResourceState
  AemAssetProperties getProperties();

  @Related(AemRelations.SLING_RESOURCE)
  SlingResource asSlingResource();

  @Related(AemRelations.RENDITION)
  Optional<AemRendition> getRendition(@TemplateVariable("width") Integer width, @TemplateVariable("height") Integer height);

  @Related(AemRelations.BINARY)
  Optional<LinkableResource> getOriginalRendition();
}
