package io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.sites;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.assets.AemAsset;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.SlingResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemLinkedContent;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemPage;

@Model(adaptables = SlingRhyme.class, adapters = AemLinkedContent.class)
public class AemLinkedContentImpl implements AemLinkedContent {

  private static final Predicate<Resource> IS_ASSET = res -> res.adaptTo(Asset.class) != null;
  private static final Predicate<Resource> IS_PAGE = res -> res.adaptTo(Page.class) != null;

  private static final Predicate<Resource> IS_OTHER = IS_ASSET.or(IS_PAGE).negate();

  @Self
  private Resource currentResource;

  @Self
  private SlingResourceAdapter resourceAdapter;

  @Override
  public Stream<AemPage> getLinkedPages() {

    return resourceAdapter
        .select(findLinkedResourcesIn(currentResource))
        .filter(IS_PAGE)
        .adaptTo(AemPage.class)
        .getStream();
  }

  @Override
  public Stream<AemAsset> getLinkedAssets() {

    return resourceAdapter
        .select(findLinkedResourcesIn(currentResource))
        .filter(IS_ASSET)
        .adaptTo(AemAsset.class)
        .getStream();
  }

  @Override
  public Stream<SlingResource> getOtherLinkedResources() {

    return resourceAdapter
        .select(findLinkedResourcesIn(currentResource))
        .filter(IS_OTHER)
        .adaptTo(SlingResource.class)
        .getStream();
  }


  private Stream<Resource> findLinkedResourcesIn(Resource contentResource) {

    ValueMap properties = contentResource.getValueMap();

    ResourceResolver resolver = currentResource.getResourceResolver();

    Stream<Resource> linkedInThisResource = properties.entrySet().stream()
        .filter(entry -> entry.getValue() instanceof String)
        .map(entry -> (String)entry.getValue())
        .filter(value -> value.startsWith("/"))
        .map(contentRef -> resolver.getResource(contentRef))
        .filter(Objects::nonNull);

    Stream<Resource> linkedInChildResources = StreamSupport.stream(contentResource.getChildren().spliterator(), false)
        .flatMap(this::findLinkedResourcesIn);

    return Stream.concat(linkedInThisResource, linkedInChildResources);
  }


}
