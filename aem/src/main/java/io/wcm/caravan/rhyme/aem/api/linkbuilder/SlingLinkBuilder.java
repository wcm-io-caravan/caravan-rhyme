package io.wcm.caravan.rhyme.aem.api.linkbuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.resources.SlingLinkableResource;


public interface SlingLinkBuilder {

  Link createLinkToCurrentResource(SlingLinkableResource slingModel);

}
