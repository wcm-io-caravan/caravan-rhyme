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
package io.wcm.caravan.rhyme.osgi.it;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;

public final class IntegrationTestEnvironment {

  private IntegrationTestEnvironment() {
    // this class contains only constants and static methods
  }

  /**
   * The fully qualified URL of the Sling Launchpad under test
   */
  public static final String ENTRY_POINT_URL = System.getProperty("launchpad.http.server.url", "http://localhost:8080");

  /**
   * @return a dynamic client proxy implementation of the {@link ExamplesEntryPointResource} interface
   */
  public static ExamplesEntryPointResource createEntryPointProxy() {

    HalApiClient client = HalApiClient.create();

    return client.getRemoteResource(ENTRY_POINT_URL, ExamplesEntryPointResource.class);
  }
}
