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
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.rhyme.api.documenation.DocumentationLoader;

@Component(service = { OsgiDocumentationLoader.class })
public class OsgiDocumentationLoader implements DocumentationLoader {

  private static final Logger log = LoggerFactory.getLogger(OsgiDocumentationLoader.class);

  private static final String BASE_URL = RhymeDocsServingApplication.BASE_PATH + "/";

  private List<Bundle> bundlesWithRhymeDocs = new ArrayList<>();

  @Override
  public String getRhymeDocsBaseUrl() {

    return BASE_URL;
  }

  @Override
  public InputStream createInputStream(String resourcePath) throws IOException {

    for (Bundle bundle : bundlesWithRhymeDocs) {

      log.info("Checking if bundle {} contains documentation file at {}", bundle.getSymbolicName(), resourcePath);
      URL resourceUrl = bundle.getResource(resourcePath);
      if (resourceUrl != null) {
        log.info("Documentation file {} was found in bundle {}", resourcePath, bundle.getSymbolicName());
        return resourceUrl.openStream();
      }
    }

    return null;
  }

  public void registerBundle(Bundle bundle) {

    bundlesWithRhymeDocs.add(bundle);
  }

  public void unregisterBundle(Bundle bundle) {

    bundlesWithRhymeDocs.remove(bundle);
  }

}
