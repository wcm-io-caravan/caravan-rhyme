package io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.sites;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;

import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.resources.AbstractLinkableResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.SlingResource;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemLinkedContent;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemPage;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.sites.AemPageProperties;

@Model(adaptables = SlingRhyme.class, adapters = AemPage.class)
public class AemPageImpl extends AbstractLinkableResource implements AemPage {

  @Self
  private Page resourcePage;

  @Override
  public AemPageProperties getProperties() {

    return new AemPageProperties() {

      @Override
      public String getTitle() {
        return resourcePage.getTitle();
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

    return resourcePage.getTitle();
  }

}
