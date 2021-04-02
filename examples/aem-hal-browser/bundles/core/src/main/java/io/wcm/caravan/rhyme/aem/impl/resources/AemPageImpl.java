package io.wcm.caravan.rhyme.aem.impl.resources;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;

import io.wcm.caravan.rhyme.aem.api.AemLinkedContent;
import io.wcm.caravan.rhyme.aem.api.AemPage;
import io.wcm.caravan.rhyme.aem.api.AemPageProperties;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.NewResourceAdapter;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, AemPage.class }, resourceType = "wcm/foundation/components/basicpage/v1/basicpage")
public class AemPageImpl extends AbstractLinkableResource implements AemPage {

  public static final String SELECTOR = "aempage";

  @RhymeObject
  private NewResourceAdapter resourceAdapter;

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
        .adaptTo(SlingResource.class)
        .withLinkTitle("Show the generic sling resource HAL representation of this page")
        .get();
  }

  @Override
  public Optional<AemPage> getParentPage() {

    return resourceAdapter
        .select().parent()
        .filter().onlyIfAdaptableTo(Page.class)
        .adaptTo(AemPage.class)
        .asOptional();
  }

  @Override
  public Stream<AemPage> getChildPages() {

    return resourceAdapter
        .select().children()
        .filter().onlyIfAdaptableTo(Page.class)
        .adaptTo(AemPage.class)
        .asStream();
  }

  @Override
  public AemLinkedContent getLinkedContent() {

    return resourceAdapter
        .select().child(JcrConstants.JCR_CONTENT)
        .adaptTo(AemLinkedContent.class)
        .get();
  }

  @Override
  protected String getDefaultLinkTitle() {

    return page.getTitle();
  }

}
