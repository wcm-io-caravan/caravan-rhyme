package io.wcm.caravan.rhyme.aem.api.adaptation;

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


  <I> PostAdaptionStage<I, I> adaptTo(Class<I> halApiInterface);

  <I, M extends I> PostAdaptionStage<I, M> adaptTo(Class<I> halApiInterface, Class<M> slingModelClass);


}
