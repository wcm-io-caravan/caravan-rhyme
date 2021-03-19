package io.wcm.caravan.rhyme.aem.integration;

import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.Nullable;


public interface SlingRhyme {

  Resource getRequestedResource();

  Resource getCurrentResource();

  RequestParameterMap getRequestParameters();

  SlingLinkBuilder getLinkBuilder();

  <@Nullable T> T adaptResource(Resource resource, Class<T> modelClass);

}
