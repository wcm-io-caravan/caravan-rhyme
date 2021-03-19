package io.wcm.caravan.rhyme.aem.integration;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;


public interface SlingLinkBuilder {

  Link createLinkToCurrentResource(LinkableResource slingModel);

}
