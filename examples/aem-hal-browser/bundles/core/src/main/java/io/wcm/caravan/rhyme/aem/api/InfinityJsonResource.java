package io.wcm.caravan.rhyme.aem.api;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@HalApiInterface
public interface InfinityJsonResource extends LinkableResource {

  @ResourceState
  ObjectNode getProperties();
}
