package io.wcm.caravan.rhyme.aem.integration;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;

public interface SlingResourceAdapter {

  SlingResourceAdapter selectCurrentResource();

  SlingResourceAdapter selectParentResource();

  SlingResourceAdapter selectChildResources();

  SlingResourceAdapter selectChildResource(String name);

  SlingResourceAdapter selectContentOfChildPages();

  SlingResourceAdapter selectContentOfChildPage(String name);

  SlingResourceAdapter selectContentOfGrandChildPages();

  SlingResourceAdapter selectLinkedResources();

  SlingResourceAdapter selectResourceAt(String path);

  SlingResourceAdapter select(Stream<Resource> resources);

  SlingResourceAdapter filter(Predicate<Resource> predicate);

  SlingResourceAdapter filterAdaptableTo(Class<?> adapterClazz);

  SlingResourceAdapter filterWithName(String resourceName);


  <SlingModelType> TypedResourceAdapter<SlingModelType> adaptTo(Class<SlingModelType> slingModelClass);


  interface TypedResourceAdapter<T> {

    TypedResourceAdapter<T> withLinkTitle(String title);

    T getInstance();

    Optional<T> getOptional();

    Stream<T> getStream();
  }


}
