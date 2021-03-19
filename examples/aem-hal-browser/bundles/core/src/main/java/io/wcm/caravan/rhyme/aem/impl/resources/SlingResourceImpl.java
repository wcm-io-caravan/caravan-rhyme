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

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.AemAsset;
import io.wcm.caravan.rhyme.aem.api.AemPage;
import io.wcm.caravan.rhyme.aem.api.InfinityJsonResource;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, SlingResource.class })
public class SlingResourceImpl implements SlingResource {

  public static final String SELECTOR = "slingresource";

  @RhymeObject
  private SlingResourceAdapter resourceAdapter;

  @RhymeObject
  private SlingLinkBuilder linkBuilder;

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
        .getSelfAs(AemPage.class);
  }

  @Override
  public Optional<AemAsset> asAemAsset() {

    return resourceAdapter
        .filter().onlyIfAdaptableTo(Asset.class)
        .getSelfAs(AemAsset.class);
  }


  @Override
  public Optional<SlingResource> getParent() {

    return resourceAdapter.getParentAs(SlingResource.class);
  }

  @Override
  public Stream<SlingResource> getChildren() {

    return resourceAdapter.getChildrenAs(SlingResource.class);
  }

  @Override
  public Optional<InfinityJsonResource> getJcrContentAsJson() {

    return resourceAdapter
        .filter().onlyIfNameIs(JcrConstants.JCR_CONTENT)
        .getChildrenAs(InfinityJsonResource.class).findFirst();
  }

  @Override
  public Link createLink() {

    String resourceType = defaultIfNull(resource.getResourceType(), "unknown");

    return linkBuilder.createLinkToCurrentResource(this)
        .setTitle(resourceType + " resource");
  }

}
