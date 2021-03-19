package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;

@Model(adaptables = SlingRhyme.class, adapters = SlingResourceAdapter.class)
public class SlingResourceAdapterImpl implements SlingResourceAdapter {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Self
  private SlingRhyme slingRhyme;

  @Self
  private Resource currentResource;

  private final List<Class> adaptersToVerify;

  public SlingResourceAdapterImpl() {
    this.adaptersToVerify = new ArrayList<>();
  }

  private SlingResourceAdapterImpl(SlingResourceAdapterImpl toClone, Class adapterToVerify) {
    this.slingRhyme = toClone.slingRhyme;
    this.currentResource = toClone.currentResource;
    this.adaptersToVerify = new ArrayList<>(toClone.adaptersToVerify);
    this.adaptersToVerify.add(adapterToVerify);
  }

  private <@Nullable T> T adaptToResourceImpl(Resource res, Class<T> resourceModelClass) {

    for (Class toVerify : adaptersToVerify) {
      Object adapted = res.adaptTo(toVerify);
      if (adapted == null) {
        return null;
      }
    }

    return slingRhyme.adaptResource(res, resourceModelClass);
  }

  private static Stream<Resource> getStreamOfChildren(Resource res) {

    return StreamSupport.stream(res.getChildren().spliterator(), false);
  }

  @Override
  public <T> T getPropertiesAs(Class<T> clazz) {

    ValueMap valueMap = currentResource.getValueMap();

    return OBJECT_MAPPER.convertValue(valueMap, clazz);
  }

  @Override
  public <T> Optional<T> getSelfAs(Class<T> resourceModelClass) {

    return getOptionalOf(currentResource, resourceModelClass);
  }

  @Override
  public <T> Optional<T> getParentAs(Class<T> resourceModelClass) {

    return getOptionalOf(currentResource.getParent(), resourceModelClass);
  }

  private <T> Optional<T> getOptionalOf(Resource resource, Class<T> resourceModelClass) {
    if (resource == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(adaptToResourceImpl(resource, resourceModelClass));
  }

  @Override
  public <T> Stream<T> getChildrenAs(Class<T> resourceModelClass) {

    return getStreamOfChildren(currentResource)
        .map(resource -> adaptToResourceImpl(resource, resourceModelClass))
        .filter(Objects::nonNull);
  }

  @Override
  public SlingResourceAdapter ifAdaptableTo(Class<?> adapterClazz) {

    return new SlingResourceAdapterImpl(this, adapterClazz);
  }
}
