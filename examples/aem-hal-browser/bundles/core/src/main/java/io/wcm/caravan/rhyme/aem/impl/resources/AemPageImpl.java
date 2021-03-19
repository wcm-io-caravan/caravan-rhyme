package io.wcm.caravan.rhyme.aem.impl.resources;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import com.day.cq.wcm.api.Page;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.AemPage;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, AemPage.class }, resourceType = "wcm/foundation/components/basicpage/v1/basicpage")
public class AemPageImpl implements AemPage {

  public static final String SELECTOR = "aempage";

  @RhymeObject
  private SlingResourceAdapter resource;

  @RhymeObject
  private SlingLinkBuilder linkBuilder;

  @Override
  public SlingResource asSlingResource() {

    return resource.getSelfAs(SlingResource.class).get();
  }

  @Override
  public Optional<AemPage> getParentPage() {

    return resource.ifAdaptableTo(Page.class).getParentAs(AemPage.class);
  }

  @Override
  public Stream<AemPage> getChildPages() {

    return resource.ifAdaptableTo(Page.class).getChildrenAs(AemPage.class);
  }

  @Override
  public Link createLink() {

    return linkBuilder.createLinkToCurrentResource(this);
  }

}
