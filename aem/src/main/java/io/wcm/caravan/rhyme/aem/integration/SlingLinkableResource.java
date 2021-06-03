package io.wcm.caravan.rhyme.aem.integration;

import java.util.Map;


public interface SlingLinkableResource {

  String getLinkTitle();

  void setLinkTitle(String linkTitle);

  Map<String, Object> getQueryParameters();

  void setQueryParameters(Map<String, Object> parameters);

}
