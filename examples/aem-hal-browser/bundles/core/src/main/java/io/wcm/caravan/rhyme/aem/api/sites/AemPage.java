package io.wcm.caravan.rhyme.aem.api.sites;

import java.util.Optional;
import java.util.stream.Stream;

import io.wcm.caravan.rhyme.aem.api.AemRelations;
import io.wcm.caravan.rhyme.aem.api.generic.SlingResource;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface AemPage extends LinkableResource {

  @ResourceState
  AemPageProperties getProperties();

  @Related(AemRelations.SLING_RESOURCE)
  SlingResource asSlingResource();

  @Related(AemRelations.PARENT)
  Optional<AemPage> getParentPage();

  @Related(AemRelations.CHILD)
  Stream<AemPage> getChildPages();

  @Related(AemRelations.LINKED_CONTENT)
  AemLinkedContent getLinkedContent();
}
