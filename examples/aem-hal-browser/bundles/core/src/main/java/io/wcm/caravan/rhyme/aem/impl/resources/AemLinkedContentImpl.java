package io.wcm.caravan.rhyme.aem.impl.resources;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;

import io.wcm.caravan.rhyme.aem.api.AemAsset;
import io.wcm.caravan.rhyme.aem.api.AemLinkedContent;
import io.wcm.caravan.rhyme.aem.api.AemPage;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;

@Model(adaptables = Resource.class, adapters = { AemLinkedContent.class })
public class AemLinkedContentImpl implements AemLinkedContent {

  private static Predicate<Resource> IS_ASSET = res -> res.adaptTo(Asset.class) != null;
  private static Predicate<Resource> IS_PAGE = res -> res.adaptTo(Page.class) != null;

  private static Predicate<Resource> IS_OTHER = IS_ASSET.or(IS_PAGE).negate();

  @RhymeObject
  private SlingResourceAdapter resourceAdapter;

  @Override
  public Stream<AemPage> getLinkedPages() {

    return resourceAdapter
        .filter().onlyMatching(IS_PAGE)
        .getLinkedAs(AemPage.class);
  }

  @Override
  public Stream<AemAsset> getLinkedAssets() {

    return resourceAdapter
        .filter().onlyMatching(IS_ASSET)
        .getLinkedAs(AemAsset.class);
  }

  @Override
  public Stream<SlingResource> getOtherLinkedResources() {

    return resourceAdapter
        .filter().onlyMatching(IS_OTHER)
        .getLinkedAs(SlingResource.class);
  }

}
