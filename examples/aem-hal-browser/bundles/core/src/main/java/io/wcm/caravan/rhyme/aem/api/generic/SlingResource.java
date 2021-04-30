package io.wcm.caravan.rhyme.aem.api.generic;

import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.rhyme.aem.api.AemRelations;
import io.wcm.caravan.rhyme.aem.api.assets.AemAsset;
import io.wcm.caravan.rhyme.aem.api.sites.AemPage;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface SlingResource extends LinkableResource {

  @ResourceState
  ObjectNode getProperties();

  @Related(AemRelations.PAGE)
  Optional<AemPage> asAemPage();

  @Related(AemRelations.ASSET)
  Optional<AemAsset> asAemAsset();

  @Related(AemRelations.INFINITY_JSON)
  Optional<InfinityJsonResource> getJcrContentAsJson();

  @Related(AemRelations.SLING_PARENT)
  Optional<SlingResource> getParent();

  @Related(AemRelations.SLING_CHILD)
  Stream<SlingResource> getChildren();

}
