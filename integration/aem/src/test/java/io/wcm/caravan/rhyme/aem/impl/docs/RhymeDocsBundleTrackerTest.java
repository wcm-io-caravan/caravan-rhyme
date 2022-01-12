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

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;


@ExtendWith(OsgiContextExtension.class)
@ExtendWith(MockitoExtension.class)
public class RhymeDocsBundleTrackerTest {

  private final OsgiContext context = new OsgiContext();

  @Mock
  private RhymeDocsOsgiBundleSupport docsSupport;

  private RhymeDocsBundleTracker tracker;

  @BeforeEach
  void setUp() {

    docsSupport = context.registerService(docsSupport);
    tracker = context.registerInjectActivateService(new RhymeDocsBundleTracker());
  }

  @AfterEach
  void tearDown() {

    MockOsgi.deactivate(tracker, context.bundleContext());
  }

  private void startBundle(Bundle bundle) {

    BundleEvent event = new BundleEvent(BundleEvent.STARTED, bundle);

    tracker.addingBundle(bundle, event);
  }

  private void stopBundle(Bundle bundle) {

    BundleEvent event = new BundleEvent(BundleEvent.STOPPING, bundle);

    tracker.removedBundle(bundle, event, bundle.getSymbolicName());
  }

  static Bundle mockBundleWithRhymeDocs() {

    String resourcePath = "/" + RhymeDocsSupport.FOLDER;

    Bundle bundle = mock(Bundle.class);

    lenient().when(bundle.getResource(resourcePath))
        .thenReturn(RhymeDocsOsgiBundleSupportTest.class.getResource(resourcePath));

    return bundle;
  }

  @Test
  public void addingBundle_shouldnt_register_bundle_without_docs() throws Exception {

    Bundle bundleWithoutDocs = Mockito.mock(Bundle.class);

    startBundle(bundleWithoutDocs);

    verifyNoInteractions(docsSupport);
  }

  @Test
  public void addingBundle_should_register_bundle_with_docs() throws Exception {

    Bundle bundleWithDocs = mockBundleWithRhymeDocs();

    startBundle(bundleWithDocs);

    verify(docsSupport).registerBundle(bundleWithDocs);
  }

  @Test
  public void modifiedBundle_should_do_nothing() throws Exception {

    Bundle bundleWithDocs = mockBundleWithRhymeDocs();

    tracker.modifiedBundle(bundleWithDocs, new BundleEvent(BundleEvent.UPDATED, bundleWithDocs), bundleWithDocs.getSymbolicName());
  }

  @Test
  public void removedBundle_should_unregister_bundle_with_docs() throws Exception {

    Bundle bundleWithDocs = mockBundleWithRhymeDocs();

    stopBundle(bundleWithDocs);

    verify(docsSupport).unregisterBundle(bundleWithDocs);
  }

  @Test
  public void removedBundle_shouldnt_unregister_bundle_without_docs() throws Exception {

    Bundle bundleWithoutDocs = Mockito.mock(Bundle.class);

    stopBundle(bundleWithoutDocs);

    verifyNoInteractions(docsSupport);
  }

}
