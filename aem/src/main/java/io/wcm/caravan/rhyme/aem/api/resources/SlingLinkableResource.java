package io.wcm.caravan.rhyme.aem.api.resources;

import java.util.Map;


public interface SlingLinkableResource {

  String getLinkTitle();

  void setLinkTitle(String linkTitle);

  String getLinkName();

  void setLinkName(String linkName);

  Map<String, Object> getQueryParameters();

  void setQueryParameters(Map<String, Object> parameters);

  void setExpandAllVariables(boolean expandAllVariables);

  boolean isExpandAllVariables();

}
