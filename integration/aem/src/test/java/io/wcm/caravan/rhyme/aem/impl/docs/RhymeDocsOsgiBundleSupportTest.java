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
package io.wcm.caravan.rhyme.aem.impl.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;

import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;


@ExtendWith(OsgiContextExtension.class)
@ExtendWith(MockitoExtension.class)
class RhymeDocsOsgiBundleSupportTest {

  private final OsgiContext context = new OsgiContext();

  private RhymeDocsOsgiBundleSupport docsSupport;

  @BeforeEach
  void setUp() {
    docsSupport = context.registerInjectActivateService(new RhymeDocsOsgiBundleSupport());
  }

  static String constructResourcePath(String fileName) {

    return "/" + RhymeDocsSupport.FOLDER + "/" + fileName;
  }

  static Bundle mockBundleWithRhymeDocsFromTestResources(String resourcePath) {

    Bundle bundle = mock(Bundle.class);

    lenient().when(bundle.getResource(resourcePath))
        .thenReturn(RhymeDocsOsgiBundleSupportTest.class.getResource(resourcePath));

    return bundle;
  }

  @Test
  void getRhymeDocsBaseUrl_should_return_base_url() {

    assertThat(docsSupport.getRhymeDocsBaseUrl())
        .isEqualTo("/content.rhymedocs.html/");
  }

  @Test
  void openResourceStream_should_return_null_if_no_bundles_were_registered() throws IOException {

    String resourcePath = constructResourcePath("Foo.html");

    assertThat(docsSupport.openResourceStream(resourcePath))
        .isNull();
  }

  @Test
  void openResourceStream_should_return_stream_if_bundle_with_matching_resource_is_registered() throws IOException {

    String resourcePath = constructResourcePath("Foo.html");

    Bundle bundle = mockBundleWithRhymeDocsFromTestResources(resourcePath);

    docsSupport.registerBundle(bundle);

    assertThat(docsSupport.openResourceStream(resourcePath))
        .isNotNull();
  }

  @Test
  void openResourceStream_should_return_null_if_bundle_with_different_file_was_registered() throws IOException {

    String fooResourcePath = constructResourcePath("Foo.html");
    String barResourcePath = constructResourcePath("Bar.html");

    Bundle fooBundle = mockBundleWithRhymeDocsFromTestResources(fooResourcePath);

    docsSupport.registerBundle(fooBundle);

    assertThat(docsSupport.openResourceStream(barResourcePath))
        .isNull();
  }

  @Test
  void openResourceStream_should_return_null_if_bundle_with_with_matching_resource_was_deregistered() throws IOException {

    String resourcePath = constructResourcePath("Foo.html");

    Bundle bundle = mockBundleWithRhymeDocsFromTestResources(resourcePath);

    docsSupport.registerBundle(bundle);
    docsSupport.unregisterBundle(bundle);

    assertThat(docsSupport.openResourceStream(resourcePath))
        .isNull();
  }

}
