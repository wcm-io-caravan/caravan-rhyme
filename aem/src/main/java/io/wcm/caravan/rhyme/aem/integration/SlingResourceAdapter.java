package io.wcm.caravan.rhyme.aem.integration;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;

public interface SlingResourceAdapter {

  SlingResourceAdapter fromCurrentPage();

  SlingResourceAdapter fromParentPage();

  SlingResourceAdapter fromGrandParentPage();

  SlingResourceAdapter fromResourceAt(String path);


  SlingResourceAdapter select(Stream<Resource> resources);

  SlingResourceAdapter selectCurrentResource();

  SlingResourceAdapter selectContentResource();

  SlingResourceAdapter selectParentResource();

  SlingResourceAdapter selectChildResources();

  SlingResourceAdapter selectChildResource(String name);

  SlingResourceAdapter selectContentOfCurrentPage();

  SlingResourceAdapter selectContentOfChildPages();

  SlingResourceAdapter selectContentOfChildPage(String name);

  SlingResourceAdapter selectContentOfGrandChildPages();

  SlingResourceAdapter selectResourceAt(String path);


  SlingResourceAdapter filter(Predicate<Resource> predicate);

  SlingResourceAdapter filterAdaptableTo(Class<?> adapterClazz);

  SlingResourceAdapter filterWithName(String resourceName);


  <SlingModelType> TypedResourceAdapter<SlingModelType> adaptTo(Class<SlingModelType> slingModelClass);


  interface TypedResourceAdapter<T> {

    TypedResourceAdapter<T> withLinkTitle(String title);

    TypedResourceAdapter<T> withQueryParameterTemplate(String... names);

    TypedResourceAdapter<T> withQueryParameters(Map<String, Object> parameters);

    T getInstance();

    Optional<T> getOptional();

    Stream<T> getStream();
  }


}
