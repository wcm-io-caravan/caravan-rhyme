package io.wcm.caravan.rhyme.aem.integration.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.wcm.caravan.rhyme.aem.integration.SlingPropertiesConverter;

@Model(adaptables = Resource.class, adapters = SlingPropertiesConverter.class)
public class SlingPropertiesConverterImpl implements SlingPropertiesConverter {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

  @Self
  private Resource currentResource;

  @Override
  public <T> T getPropertiesAs(Class<T> clazz) {

    ValueMap valueMap = currentResource.getValueMap();

    return OBJECT_MAPPER.convertValue(valueMap, clazz);
  }

}
