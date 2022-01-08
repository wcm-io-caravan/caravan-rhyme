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
package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import io.wcm.caravan.rhyme.testing.spring.MockMvcHalResourceLoaderConfiguration;

/**
 * This test verifies the API functionality when being used from within the same application.
 * It executes all tests defined in {@link AbstractCompanyApiIT} directly against the server-side
 * resource implementations. It does not verify the link generation or JSON (de)serialization as the
 * {@link ExternalClientIT} or {@link MockMvcClientIT} do.
 */
@SpringBootTest
@Import(MockMvcHalResourceLoaderConfiguration.class)
public class ServerSideIT extends AbstractCompanyApiIT {

  @Autowired
  private CompanyApiController api;

  @Override
  protected CompanyApi getApiImplementionOrClientProxy() {
    return api.get();
  }
}
