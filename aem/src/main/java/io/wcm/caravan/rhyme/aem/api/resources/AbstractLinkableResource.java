package io.wcm.caravan.rhyme.aem.api.resources;

import javax.annotation.PostConstruct;

import org.apache.sling.models.annotations.injectorspecific.Self;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.aem.api.adaptation.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.LinkProperties;
import io.wcm.caravan.rhyme.aem.api.linkbuilder.SlingLinkBuilder;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * An abstract implementation of {@link LinkableResource}, that you can use as a super class
 * for your sling models implementing your {@link HalApiInterface}s. It provides injected fields with
 * the commonly used {@link SlingRhyme} and {@link SlingResourceAdapter} sling models, and
 * also contains an implementation of {@link LinkableResource#createLink()} that uses
 * the {@link SlingLinkBuilder} to generate the link.
 */
public abstract class AbstractLinkableResource implements LinkableResource, SlingLinkableResource {

  @Self
  protected SlingRhyme rhyme;

  @Self
  protected SlingResourceAdapter resourceAdapter;

  @Self
  protected SlingLinkBuilder linkBuilder;

  private final LinkProperties linkProperties = new LinkProperties();

  @PostConstruct
  void init() {
    linkProperties.setTitle(getDefaultLinkTitle());
    linkProperties.setName(rhyme.getCurrentResource().getName());
  }

  @Override
  public Link createLink() {

    return linkBuilder.createLinkToCurrentResource(this);
  }

  @Override
  public LinkProperties getLinkProperties() {

    return linkProperties;
  }

  protected abstract String getDefaultLinkTitle();

}
