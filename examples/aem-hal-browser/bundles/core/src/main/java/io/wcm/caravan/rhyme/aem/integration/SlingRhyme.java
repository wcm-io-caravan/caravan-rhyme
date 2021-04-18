package io.wcm.caravan.rhyme.aem.integration;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public interface SlingRhyme extends Adaptable {

  Resource getRequestedResource();

  Resource getCurrentResource();

  RequestParameterMap getRequestParameters();

  <@Nullable T> T adaptResource(Resource resource, @NotNull Class<T> modelClass);


}
