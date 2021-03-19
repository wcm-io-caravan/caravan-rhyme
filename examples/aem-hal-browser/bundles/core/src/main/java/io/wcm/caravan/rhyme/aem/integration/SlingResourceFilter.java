package io.wcm.caravan.rhyme.aem.integration;

import java.util.function.Predicate;

import org.apache.sling.api.resource.Resource;

public interface SlingResourceFilter {

  SlingResourceAdapter onlyIfAdaptableTo(Class<?> adapterClazz);

  SlingResourceAdapter onlyIfNameIs(String resourceName);

  SlingResourceAdapter onlyMatching(Predicate<Resource> predicate);
}
