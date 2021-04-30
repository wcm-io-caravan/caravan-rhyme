package io.wcm.caravan.rhyme.aem.integration;

import java.time.Duration;
import java.util.Map;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;


public interface SlingRhyme extends Adaptable {

  Resource getRequestedResource();

  Resource getCurrentResource();

  RequestParameterMap getRequestParameters();

  <@Nullable T> T adaptResource(Resource resource, @NotNull Class<T> modelClass);

  HalResponse renderRequestedResource(Map<String, Class<? extends LinkableResource>> selectorModelClassMap);

  HalResponse renderResource(LinkableResource resource);

  void setResponseMaxAge(Duration duration);

  <T> T getRemoteResource(String uri, Class<T> halApiInterface);

}
