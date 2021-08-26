package io.wcm.caravan.rhyme.aem.api;

import java.time.Duration;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;


public interface SlingRhyme extends Adaptable {

  Resource getRequestedResource();

  Resource getCurrentResource();

  <@NotNull T> T adaptResource(Resource resource, @NotNull Class<T> modelClass);

  void setResponseMaxAge(Duration duration);

  <T> T getRemoteResource(String uri, Class<T> halApiInterface);

}
