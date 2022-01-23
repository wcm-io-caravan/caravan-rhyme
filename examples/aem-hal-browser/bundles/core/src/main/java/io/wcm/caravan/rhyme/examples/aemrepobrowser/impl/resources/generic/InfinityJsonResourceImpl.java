package io.wcm.caravan.rhyme.examples.aemrepobrowser.impl.resources.generic;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingRhyme;
import io.wcm.caravan.rhyme.examples.aemrepobrowser.api.generic.InfinityJsonResource;
import io.wcm.caravan.rhyme.tooling.annotations.ExcludeFromJacocoGeneratedReport;
import io.wcm.handler.url.UrlHandler;

@Model(adaptables = SlingRhyme.class, adapters = InfinityJsonResource.class)
public class InfinityJsonResourceImpl implements InfinityJsonResource {

  @Self
  private UrlHandler urlHandler;

  @Self
  private Resource currentResource;

  @ExcludeFromJacocoGeneratedReport
  @Override
  public ObjectNode getProperties() {

    throw new NotImplementedException("This method doesn't need to be implemented and should never be called, "
        + "because createLink() builds links to the built-in servlet from AEM.");
  }

  @Override
  public Link createLink() {

    String url = urlHandler.get(currentResource)
        .selectors("infinity")
        .extension("json")
        .buildExternalResourceUrl();

    return new Link(url)
        .setTitle("AEM's built-in infinity.json representation of the jcr:content node of this resource");
  }
}
