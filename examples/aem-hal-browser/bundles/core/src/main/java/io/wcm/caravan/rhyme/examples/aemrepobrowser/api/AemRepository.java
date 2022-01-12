package io.wcm.caravan.rhyme.examples.aemrepobrowser.api;

import java.util.Optional;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemAsset;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemRendition;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.SlingResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemPage;

@HalApiInterface
public interface AemRepository {

  @Related(AemRelations.SLING_ROOT)
  SlingResource getRoot();

  @Related(AemRelations.SLING_RESOURCE)
  Optional<SlingResource> getResource(String path);

  @Related(AemRelations.PAGE)
  Optional<AemPage> getPage(String path);

  @Related(AemRelations.ASSET)
  Optional<AemAsset> getAsset(String path);

  @Related(AemRelations.RENDITION)
  Optional<AemRendition> getRendition(String path, Integer width, Integer height);

}
