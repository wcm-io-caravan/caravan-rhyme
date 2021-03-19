package io.wcm.caravan.rhyme.aem.impl.resources;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.AemLinkedContent;
import io.wcm.caravan.rhyme.aem.api.AemPage;
import io.wcm.caravan.rhyme.aem.api.AemPageProperties;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, AemPage.class }, resourceType = "wcm/foundation/components/basicpage/v1/basicpage")
public class AemPageImpl implements AemPage {

  public static final String SELECTOR = "aempage";

  @RhymeObject
  private SlingResourceAdapter resourceAdapter;

  @RhymeObject
  private SlingLinkBuilder linkBuilder;

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

    return resourceAdapter.getSelfAs(SlingResource.class).get();
  }

  @Override
  public Optional<AemPage> getParentPage() {

    return resourceAdapter.filter().onlyIfAdaptableTo(Page.class)
        .getParentAs(AemPage.class);
  }

  @Override
  public Stream<AemPage> getChildPages() {

    return resourceAdapter.filter().onlyIfAdaptableTo(Page.class)
        .getChildrenAs(AemPage.class);
  }

  @Override
  public AemLinkedContent getLinkedContent() {

    return resourceAdapter.filter().onlyIfNameIs(JcrConstants.JCR_CONTENT)
        .getChildrenAs(AemLinkedContent.class)
        .findFirst()
        .orElseThrow(() -> new HalApiDeveloperException("Failed to find a jcr:content node for a cq:Page resource"));
  }

  @Override
  public Link createLink() {

    return linkBuilder.createLinkToCurrentResource(this)
        .setTitle(page.getTitle());
  }


}
