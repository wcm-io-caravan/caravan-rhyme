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

import org.junit.jupiter.api.Test;

import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithFieldProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithNestedProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRecursiveObjectTypes;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRepeatedLists;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRepeatedObjectTypes;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.ResourceWithRxBeanProperties;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestEntryPoint;
import io.wcm.caravan.maven.plugins.rhymedocs.interfaces.RhymeDocTestResource;


public class RhymeApiDocsTest {

  public static final String[] TEST_INTERFACES = {
      RhymeDocTestEntryPoint.class.getName(),
      RhymeDocTestResource.class.getName(),
      ResourceWithFieldProperties.class.getName(),
      ResourceWithRxBeanProperties.class.getName(),
      ResourceWithNestedProperties.class.getName(),
      ResourceWithRepeatedObjectTypes.class.getName(),
      ResourceWithRecursiveObjectTypes.class.getName(),
      ResourceWithRepeatedLists.class.getName()
  };

  static RhymeApiDocs getApiDocs() {
    return new RhymeApiDocs(Paths.get("src/test/java"), RhymeApiDocsTest.class.getClassLoader());
  }

  static RhymeResourceDocs getDocsFor(Class<?> halApiInterface) {

    RhymeApiDocs apiDocs = getApiDocs();

    return apiDocs.getResourceDocs().stream()
        .filter(docs -> docs.getFullyQualifiedClassName().equals(halApiInterface.getName()))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("No documentation was generated for " + halApiInterface));
  }

  @Test
  public void getResourceDocs_should_return_docs_for_all_HalApiInterfaces() throws Exception {

    List<RhymeResourceDocs> resourceDocs = getApiDocs().getResourceDocs();

    assertThat(resourceDocs).hasSize(TEST_INTERFACES.length);

    assertThat(resourceDocs).extracting(RhymeResourceDocs::getFullyQualifiedClassName)
        .containsExactlyInAnyOrder(TEST_INTERFACES);
  }

}
