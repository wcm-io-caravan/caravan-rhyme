package io.wcm.caravan.rhyme.aem.impl.resources;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.rhyme.aem.api.AemAsset;
import io.wcm.caravan.rhyme.aem.api.AemPage;
import io.wcm.caravan.rhyme.aem.api.InfinityJsonResource;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.AbstractLinkableResource;
import io.wcm.caravan.rhyme.aem.integration.NewResourceAdapter;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, SlingResource.class })
public class SlingResourceImpl extends AbstractLinkableResource implements SlingResource {

  public static final String SELECTOR = "slingresource";

  @RhymeObject
  private NewResourceAdapter resourceAdapter;

  @Self
  private Resource resource;

  @Override
  public JsonNode getProperties() {

    return resourceAdapter.getPropertiesAs(ObjectNode.class);
  }

  @Override
  public Optional<AemPage> asAemPage() {

    return resourceAdapter
        .filter().onlyIfAdaptableTo(Page.class)
        .adaptTo(AemPage.class)
        .withLinkTitle("Show the specific HAL representation for this AEM page")
        .asOptional();
  }

  @Override
  public Optional<AemAsset> asAemAsset() {

    return resourceAdapter
        .filter().onlyIfAdaptableTo(Asset.class)
        .adaptTo(AemAsset.class)
        .asOptional();
  }

  @Override
  public Optional<SlingResource> getParent() {

    return resourceAdapter
        .select().parent()
        .adaptTo(SlingResource.class)
        .asOptional();
  }

  @Override
  public Stream<SlingResource> getChildren() {

    return resourceAdapter
        .select().children()
        .adaptTo(SlingResource.class)
        .asStream();
  }

  @Override
  public Optional<InfinityJsonResource> getJcrContentAsJson() {

    return resourceAdapter
        .select().child(JcrConstants.JCR_CONTENT)
        .adaptTo(InfinityJsonResource.class)
        .asOptional();
  }

  @Override
  protected String getDefaultLinkTitle() {

    String resourceType = defaultIfNull(resource.getResourceType(), "unknown");

    return resourceType + " resource";
  }

}
