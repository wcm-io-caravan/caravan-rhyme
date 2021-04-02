package io.wcm.caravan.rhyme.aem.integration;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;

public interface NewResourceAdapter {

  ResourceSelector select();

  ResourceFilter filter();

  <ModelType> ResourceAdapter<ModelType> adaptTo(Class<ModelType> clazz);

  <T> T getPropertiesAs(Class<T> clazz);

  interface ResourceSelector {

    NewResourceAdapter children();

    NewResourceAdapter parent();

    NewResourceAdapter child(String name);
  }

  interface ResourceFilter {

    NewResourceAdapter onlyIfAdaptableTo(Class<?> adapterClazz);

    NewResourceAdapter onlyIfNameIs(String resourceName);

    NewResourceAdapter onlyMatching(Predicate<Resource> predicate);
  }

  interface ResourceAdapter<T> {

    ResourceAdapter<T> withLinkTitle(String title);

    T get();

    Optional<T> asOptional();

    Stream<T> asStream();
  }


}
