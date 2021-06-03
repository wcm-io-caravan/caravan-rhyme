package io.wcm.caravan.rhyme.aem.impl.resources.sites;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;

import io.wcm.caravan.rhyme.aem.api.generic.SlingResource;
import io.wcm.caravan.rhyme.aem.api.sites.AemLinkedContent;
import io.wcm.caravan.rhyme.aem.api.sites.AemPage;
import io.wcm.caravan.rhyme.aem.api.sites.AemPageProperties;
import io.wcm.caravan.rhyme.aem.integration.AbstractLinkableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, AemPage.class })
public class AemPageImpl extends AbstractLinkableResource implements AemPage {


  @Self
  private Page page;

  @Override
  public AemPageProperties getProperties() {

    return new AemPageProperties() {

      @Override
      public String getTitle() {
        return page.getTitle();
      }
    };
  }

  @Override
  public SlingResource asSlingResource() {

    return resourceAdapter
        .selectCurrentResource()
        .adaptTo(SlingResource.class)
        .withLinkTitle("Show the generic sling resource HAL representation of this page")
        .getInstance();
  }

  @Override
  public Optional<AemPage> getParentPage() {

    return resourceAdapter
        .selectParentResource()
        .filterAdaptableTo(Page.class)
        .adaptTo(AemPage.class)
        .getOptional();
  }

  @Override
  public Stream<AemPage> getChildPages() {

    return resourceAdapter
        .selectChildResources()
        .filterAdaptableTo(Page.class)
        .adaptTo(AemPage.class)
        .getStream();
  }

  @Override
  public AemLinkedContent getLinkedContent() {

    return resourceAdapter
        .selectChildResource(JcrConstants.JCR_CONTENT)
        .adaptTo(AemLinkedContent.class)
        .getInstance();
  }

  @Override
  protected String getDefaultLinkTitle() {

    return page.getTitle();
  }

}
