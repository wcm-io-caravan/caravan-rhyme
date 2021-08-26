package io.wcm.caravan.rhyme.aem.api.adaptation;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
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

  <T> SlingResourceAdapter filterAdaptableTo(Class<T> adapterClazz, Predicate<T> predicate);

  SlingResourceAdapter filterWithName(String resourceName);


  <I> TypedResourceAdapter<I, I> adaptTo(Class<I> halApiInterface);

  <I, M extends I> TypedResourceAdapter<I, M> adaptTo(Class<I> halApiInterface, Class<M> slingModelClass);


  interface TypedResourceAdapter<I, M extends I> {

    TypedResourceAdapter<I, M> withModifications(Consumer<M> consumer);

    TypedResourceAdapter<I, M> withLinkTitle(String title);

    TypedResourceAdapter<I, M> withLinkName(String name);

    TypedResourceAdapter<I, M> withQueryParameterTemplate(String... names);

    TypedResourceAdapter<I, M> withQueryParameters(Map<String, Object> parameters);

    TypedResourceAdapter<I, M> withPartialLinkTemplate();

    M getInstance();

    Optional<I> getOptional();

    Stream<I> getStream();
  }


}
