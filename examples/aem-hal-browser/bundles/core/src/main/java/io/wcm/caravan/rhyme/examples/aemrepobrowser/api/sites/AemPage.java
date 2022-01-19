package io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites;

import java.util.Optional;
import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.AemRelations;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.SlingResource;

@HalApiInterface
public interface AemPage extends LinkableResource {

  @ResourceProperty
  String getTitle();

  @Related(AemRelations.SLING_RESOURCE)
  SlingResource asSlingResource();

  @Related(AemRelations.PARENT)
  Optional<AemPage> getParentPage();

  @Related(AemRelations.CHILD)
  Stream<AemPage> getChildPages();

  @Related(AemRelations.LINKED_CONTENT)
  AemLinkedContent getLinkedContent();
}
