package io.wcm.caravan.rhyme.aem.impl.resources;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.AemPage;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.RhymeObject;
import io.wcm.caravan.rhyme.aem.integration.SlingLinkBuilder;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class, adapters = { LinkableResource.class, SlingResource.class })
public class SlingResourceImpl implements SlingResource {

  public static final String SELECTOR = "slingresource";

  @RhymeObject
  private SlingResourceAdapter resource;

  @RhymeObject
  private SlingLinkBuilder linkBuilder;

  @Override
  public JsonNode getProperties() {

    return resource.getPropertiesAs(JsonNode.class);
  }

  @Override
  public Optional<AemPage> asAemPage() {

    return resource.ifAdaptableTo(Page.class).getSelfAs(AemPage.class);
  }

  @Override
  public Optional<SlingResource> getParent() {

    return resource.getParentAs(SlingResource.class);
  }

  @Override
  public Stream<SlingResource> getChildren() {

    return resource.getChildrenAs(SlingResource.class);
  }

  @Override
  public Link createLink() {

    return linkBuilder.createLinkToCurrentResource(this);
  }
}
