package io.wcm.caravan.rhyme.aem.impl.resources.sites;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;

import io.wcm.caravan.rhyme.aem.api.assets.AemAsset;
import io.wcm.caravan.rhyme.aem.api.generic.SlingResource;
import io.wcm.caravan.rhyme.aem.api.sites.AemLinkedContent;
import io.wcm.caravan.rhyme.aem.api.sites.AemPage;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;

@Model(adaptables = Resource.class, adapters = { AemLinkedContent.class })
public class AemLinkedContentImpl implements AemLinkedContent {

  private static final Predicate<Resource> IS_ASSET = res -> res.adaptTo(Asset.class) != null;
  private static final Predicate<Resource> IS_PAGE = res -> res.adaptTo(Page.class) != null;

  private static final Predicate<Resource> IS_OTHER = IS_ASSET.or(IS_PAGE).negate();

  @RhymeObject
  private SlingResourceAdapter resourceAdapter;

  @Override
  public Stream<AemPage> getLinkedPages() {

    return resourceAdapter
        .selectLinkedResources()
        .filter(IS_PAGE)
        .adaptTo(AemPage.class)
        .getStream();
  }

  @Override
  public Stream<AemAsset> getLinkedAssets() {

    return resourceAdapter
        .selectLinkedResources()
        .filter(IS_ASSET)
        .adaptTo(AemAsset.class)
        .getStream();
  }

  @Override
  public Stream<SlingResource> getOtherLinkedResources() {

    return resourceAdapter
        .selectLinkedResources()
        .filter(IS_OTHER)
        .adaptTo(SlingResource.class)
        .getStream();
  }

}
