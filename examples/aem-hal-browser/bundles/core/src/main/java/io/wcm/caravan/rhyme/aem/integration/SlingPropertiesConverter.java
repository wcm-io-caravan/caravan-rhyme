package io.wcm.caravan.rhyme.aem.integration;


public interface SlingPropertiesConverter {

  <T> T getPropertiesAs(Class<T> clazz);

}
