package io.wcm.caravan.rhyme.aem.impl.resources;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.InfinityJsonResource;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = Resource.class, adapters = { InfinityJsonResource.class })
public class InfinityJsonResourceImpl implements InfinityJsonResource {

  @Self
  private UrlHandler urlHandler;

  @Self
  private Resource currentResource;

  @Override
  public ObjectNode getProperties() {
    throw new NotImplementedException("This method doesn't need to be implemented");
  }

  @Override
  public Link createLink() {

    String url = urlHandler.get(currentResource)
        .selectors("infinity")
        .extension("json")
        .buildExternalResourceUrl();

    return new Link(url)
        .setTitle("The native infinity.json representation of the jcr:content node of this resource");
  }


}
