package io.wcm.caravan.rhyme.aem.integration;

import java.util.Optional;
import java.util.stream.Stream;

public interface SlingResourceAdapter {

  <T> Optional<T> getSelfAs(Class<T> resourceModelClass);

  <T> Stream<T> getChildrenAs(Class<T> resourceModelClass);

  <T> Optional<T> getParentAs(Class<T> resourceModelClass);

  <T> T getPropertiesAs(Class<T> clazz);

  SlingResourceAdapter ifAdaptableTo(Class<?> adapterClazz);

}
