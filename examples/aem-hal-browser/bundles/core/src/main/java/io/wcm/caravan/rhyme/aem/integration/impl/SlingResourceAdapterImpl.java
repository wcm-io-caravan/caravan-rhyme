package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceFilter;
import io.wcm.caravan.rhyme.aem.integration.SlingRhyme;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

@Model(adaptables = SlingRhyme.class, adapters = SlingResourceAdapter.class)
public class SlingResourceAdapterImpl implements SlingResourceAdapter {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

  @Self
  private SlingRhyme slingRhyme;

  @Self
  private Resource currentResource;

  private final List<Predicate<Resource>> adaptersToVerify;

  public SlingResourceAdapterImpl() {
    this.adaptersToVerify = new ArrayList<>();
  }

  private SlingResourceAdapterImpl(SlingResourceAdapterImpl toClone, Predicate<Resource> additionalPredicate) {
    this.slingRhyme = toClone.slingRhyme;
    this.currentResource = toClone.currentResource;
    this.adaptersToVerify = new ArrayList<>(toClone.adaptersToVerify);
    this.adaptersToVerify.add(additionalPredicate);
  }

  private SlingResourceAdapterImpl(SlingResourceAdapterImpl toClone, Resource currentResource) {
    this.slingRhyme = toClone.slingRhyme;
    this.currentResource = currentResource;
    this.adaptersToVerify = new ArrayList<>(toClone.adaptersToVerify);
  }

  private <@Nullable T> T adaptToResourceImpl(Resource res, Class<T> resourceModelClass) {

    for (Predicate<Resource> predicate : adaptersToVerify) {
      if (!predicate.test(res)) {
        return null;
      }
    }

    return slingRhyme.adaptResource(res, resourceModelClass);
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

    return ResourceUtils.getStreamOfChildren(currentResource)
        .map(resource -> adaptToResourceImpl(resource, resourceModelClass))
        .filter(Objects::nonNull);
  }

  @Override
  public <T> Stream<T> getLinkedAs(Class<T> resourceModelClass) {

    return findLinkedResourcesIn(currentResource)
        .map(resource -> adaptToResourceImpl(resource, resourceModelClass))
        .filter(Objects::nonNull);
  }

  private Stream<Resource> findLinkedResourcesIn(Resource contentResource) {

    ValueMap properties = contentResource.getValueMap();

    ResourceResolver resolver = currentResource.getResourceResolver();

    Stream<Resource> linkedInThisResource = properties.entrySet().stream()
        .filter(entry -> entry.getValue() instanceof String)
        .map(entry -> (String)entry.getValue())
        .filter(value -> value.startsWith("/"))
        .map(contentRef -> resolver.getResource(contentRef))
        .filter(Objects::nonNull);

    Stream<Resource> linkedInChildResources = StreamSupport.stream(contentResource.getChildren().spliterator(), false)
        .flatMap(this::findLinkedResourcesIn);

    return Stream.concat(linkedInThisResource, linkedInChildResources);
  }

  @Override
  public SlingResourceFilter filter() {
    return new SlingResourceFilter() {

      @Override
      public SlingResourceAdapter onlyMatching(Predicate<Resource> predicate) {
        return new SlingResourceAdapterImpl(SlingResourceAdapterImpl.this, predicate);
      }

      @Override
      public SlingResourceAdapter onlyIfNameIs(String resourceName) {
        return onlyMatching((res) -> res.getName().equals(resourceName));
      }

      @Override
      public SlingResourceAdapter onlyIfAdaptableTo(Class<?> adapterClazz) {
        return onlyMatching((res) -> res.adaptTo(adapterClazz) != null);
      }
    };
  }

  @Override
  public SlingResourceAdapter withDifferentResource(String path) {

    Resource child = currentResource.getResourceResolver().getResource(path);
    if (child == null) {
      throw new HalApiDeveloperException("No resource exists at path " + path);
    }

    return new SlingResourceAdapterImpl(this, child);
  }


}
