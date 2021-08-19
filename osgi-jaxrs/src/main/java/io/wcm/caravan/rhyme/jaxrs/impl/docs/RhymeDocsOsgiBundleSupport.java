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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;

/**
 * OSGI specific implementation of {@link RhymeDocsSupport} that will load HTML documentation
 * generated with the rhyme-docs-maven-plugin from the bundle resources.
 */
@Component(service = { RhymeDocsOsgiBundleSupport.class })
public class RhymeDocsOsgiBundleSupport implements RhymeDocsSupport {

  private static final Logger log = LoggerFactory.getLogger(RhymeDocsOsgiBundleSupport.class);

  private static final String BASE_URL = RhymeDocsJaxRsApplication.BASE_PATH + "/";

  private List<Bundle> bundlesWithRhymeDocs;

  @Activate
  void activate() {
    bundlesWithRhymeDocs = new ArrayList<>();
  }

  @Override
  public String getRhymeDocsBaseUrl() {

    return BASE_URL;
  }

  @Override
  public InputStream openResourceStream(String resourcePath) throws IOException {

    for (Bundle bundle : bundlesWithRhymeDocs) {

      log.debug("Checking if bundle {} contains documentation file at {}", bundle.getSymbolicName(), resourcePath);
      URL resourceUrl = bundle.getResource(resourcePath);
      if (resourceUrl != null) {
        log.debug("Documentation file {} was found in bundle {}", resourcePath, bundle.getSymbolicName());
        return resourceUrl.openStream();
      }
    }

    return null;
  }

  @Override
  public boolean isFragmentAppendedToCuriesLink() {
    // do not use fragments in documentation URL since the HAL browser frontend,
    // as opening those links prevents scrolling of the HAL browser application content in the browser
    return false;
  }


  /**
   * Called by the {@link RhymeDocsBundleTracker} if relevant documentation was found in a bundle
   * @param bundle a bundle known to contain a {@link RhymeDocsSupport#FOLDER} folder
   */
  void registerBundle(Bundle bundle) {

    bundlesWithRhymeDocs.add(bundle);
  }

  /**
   * Call by the {@link RhymeDocsBundleTracker} if a bundle has been de-activated
   * @param bundle any OSGI bundle
   */
  void unregisterBundle(Bundle bundle) {

    bundlesWithRhymeDocs.remove(bundle);
  }

}
