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
package io.wcm.caravan.rhyme.jaxrs.impl.docs;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;

/**
 * Tracks all bundle activations to check if a bundle contains HTML documentation resources
 * generated with the rhyme-docs-maven-plugin, so that the {@link RhymeDocsOsgiBundleSupport}
 * knows in which bundles to look for these documentation files
 */
@Component(immediate = true)
public class RhymeDocsBundleTracker implements BundleTrackerCustomizer<String> {

  private static final Logger log = LoggerFactory.getLogger(RhymeDocsBundleTracker.class);

  @Reference
  private RhymeDocsOsgiBundleSupport rhymeDocsSupport;

  private BundleTracker bundleTracker;

  @Activate
  void activate(ComponentContext componentContext) {

    BundleContext bundleContext = componentContext.getBundleContext();

    bundleTracker = new BundleTracker<String>(bundleContext, Bundle.ACTIVE, this);
    bundleTracker.open();
  }

  @Deactivate
  void deactivate(@SuppressWarnings("unused") ComponentContext componentContext) {

    bundleTracker.close();
  }

  private static boolean hasRhymeDocs(Bundle bundle) {

    String rhymeDocsPath = "/" + RhymeDocsSupport.FOLDER;

    URL rhymeDocsUrl = bundle.getResource(rhymeDocsPath);

    if (rhymeDocsUrl == null) {
      log.debug("No rhyme docs were found in bundle at {}", bundle.getSymbolicName(), rhymeDocsPath);
      return false;
    }

    log.info("Rhyme docs were found in bundle {} at {}", bundle.getSymbolicName(), rhymeDocsUrl);
    return true;
  }
  
  @Override
  public String addingBundle(Bundle bundle, BundleEvent event) {

    log.debug("Bundle {} was added", bundle.getSymbolicName());

    if (!hasRhymeDocs(bundle)) {
      return null;
    }

    rhymeDocsSupport.registerBundle(bundle);
  
    return bundle.getSymbolicName();  
  }

  @Override
  public void modifiedBundle(Bundle bundle, BundleEvent event, String symbolicName) {
    // nothing to do
  }

  @Override
  public void removedBundle(Bundle bundle, BundleEvent event, String symbolicName) {

    if (!hasRhymeDocs(bundle)) {
      return;
    }

    rhymeDocsSupport.unregisterBundle(bundle);
  }

}
