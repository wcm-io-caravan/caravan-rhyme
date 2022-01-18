/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.maven.plugins.rhymedocs.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithFieldProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithLinksInJavadoc;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRxBeanProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestEntryPoint;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestResource;


public class RhymeApiDocsTest {

  public static final Class[] TEST_INTERFACES = {
      RhymeDocTestEntryPoint.class,
      RhymeDocTestResource.class,
      ResourceWithFieldProperties.class,
      ResourceWithRxBeanProperties.class,
      ResourceWithLinksInJavadoc.class,
      RhymePropertiesDocsTest.ResourceWithNestedProperties.class,
      RhymePropertiesDocsTest.ResourceWithRepeatedObjectTypes.class,
      RhymePropertiesDocsTest.ResourceWithRecursiveObjectTypes.class,
      RhymePropertiesDocsTest.ResourceWithRepeatedLists.class,
      RhymePropertiesDocsTest.ResourceWithJsonProperties.class,
      RhymePropertiesDocsTest.ResourceWithEdgeCaseProperties.class,
      RhymePropertiesDocsTest.ResourceWithArrayProperties.class,
      RhymePropertiesDocsTest.ResourceWithPropertiesWithoutSource.class,
      RhymePropertiesDocsTest.ResourceWithAnnotatedProperties.class,
      RhymePropertiesDocsTest.ResourceWithJsonIgnoreAnnotation.class,
      RhymePropertiesDocsTest.ResourceWithPrivateFieldsAndBeanProperties.class,
      RhymePropertiesDocsTest.ResourceWithResourceProperty.class
  };

  static RhymeApiDocs getApiDocs() {
    return new RhymeApiDocs(Paths.get("src/test/java"), RhymeApiDocsTest.class.getClassLoader());
  }

  static RhymeResourceDocs getDocsFor(Class<?> halApiInterface) {

    RhymeApiDocs apiDocs = getApiDocs();

    return apiDocs.getResourceDocs().stream()
        .filter(docs -> docs.getCanonicalClassName().equals(halApiInterface.getCanonicalName()))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("No documentation was generated for " + halApiInterface));
  }

  @Test
  void getResourceDocs_should_return_docs_for_all_HalApiInterfaces()  {

    List<RhymeResourceDocs> resourceDocs = getApiDocs().getResourceDocs();

    assertThat(resourceDocs).hasSize(TEST_INTERFACES.length);

    String[] expectedNames = Stream.of(TEST_INTERFACES)
        .map(Class::getCanonicalName)
        .toArray(String[]::new);

    assertThat(resourceDocs).extracting(RhymeResourceDocs::getCanonicalClassName)
        .containsExactlyInAnyOrder(expectedNames);
  }

}
