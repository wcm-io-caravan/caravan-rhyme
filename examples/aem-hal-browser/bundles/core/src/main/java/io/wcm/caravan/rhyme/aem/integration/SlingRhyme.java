package io.wcm.caravan.rhyme.aem.integration;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = SlingHttpServletRequest.class)
public class SlingRhyme {

  @Self
  private SlingHttpServletRequest request;

  private Rhyme rhyme;

  @PostConstruct
  void init() {
    rhyme = RhymeBuilder.withoutResourceLoader()
        .buildForRequestTo(request.getRequestURL().toString());
  }


  public HalResponse renderResponseForCurrentResource() {

    LinkableResource resourceImpl = request.getResource().adaptTo(LinkableResource.class);
    if (resourceImpl == null) {
      throw new RuntimeException("Failed to adapt resource of current request");
    }

    return rhyme.renderResponse(resourceImpl);
  }
}
