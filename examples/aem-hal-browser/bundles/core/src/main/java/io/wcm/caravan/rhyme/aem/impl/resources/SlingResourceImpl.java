package io.wcm.caravan.rhyme.aem.impl.resources;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.aem.api.SlingResource;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Model(adaptables = Resource.class, adapters = LinkableResource.class)
public class SlingResourceImpl implements SlingResource {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Self
  private Resource resource;

  private SlingRhyme rhyme;

  @Override
  public JsonNode getProperties() {

    ValueMap valueMap = resource.getValueMap();

    return OBJECT_MAPPER.convertValue(valueMap, JsonNode.class);
  }

  @Override
  public Optional<SlingResource> getParent() {

    Resource parent = resource.getParent();
    if (parent == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(adaptToResourceImpl(parent));
  }

  private @Nullable SlingResource adaptToResourceImpl(Resource res) {

    return rhyme.adaptResource(res, SlingResourceImpl.class);
  }

  private static Stream<Resource> getStreamOfChildren(Resource res) {

    return StreamSupport.stream(res.getChildren().spliterator(), false);
  }

  @Override
  public Stream<SlingResource> getChildren() {

    return getStreamOfChildren(resource)
        .sorted((r1, r2) -> r1.getName().compareTo(r2.getName()))
        .map(this::adaptToResourceImpl)
        .filter(Objects::nonNull);
  }


  @Override
  public Link createLink() {

    return rhyme.getLinkBuilder().createLinkToCurrentResource();
  }
}
