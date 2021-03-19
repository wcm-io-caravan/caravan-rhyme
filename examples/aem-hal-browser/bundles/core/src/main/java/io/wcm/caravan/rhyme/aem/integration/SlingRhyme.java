package io.wcm.caravan.rhyme.aem.integration;

import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;


public interface SlingRhyme {

  Resource getRequestedResource();

  Resource getCurrentResource();

  RequestParameterMap getRequestParameters();

  SlingLinkBuilder getLinkBuilder();

  <T> T adaptResource(Resource resource, Class<T> modelClass);

}
