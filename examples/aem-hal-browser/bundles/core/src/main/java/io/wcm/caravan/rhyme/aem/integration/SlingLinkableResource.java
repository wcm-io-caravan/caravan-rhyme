package io.wcm.caravan.rhyme.aem.integration;

import java.util.Collections;
import java.util.Map;


public interface SlingLinkableResource {

  String getLinkTitle();

  default Map<String, Object> getQueryParameters() {
    return Collections.emptyMap();
  };

}
