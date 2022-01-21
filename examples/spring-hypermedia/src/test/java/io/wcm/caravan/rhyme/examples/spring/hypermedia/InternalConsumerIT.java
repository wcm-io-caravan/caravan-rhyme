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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.testing.spring.MockMvcHalResourceLoaderConfiguration;

/**
 * This test verifies the API functionality when being used from within the same application.
 * It executes all tests defined in {@link AbstractCompanyApiIT} directly against the server-side
 * resource implementations. It does not verify the link generation or JSON (de)serialization as the
 * {@link ExternalClientIT} or {@link MockMvcClientIT} do.
 */
@SpringBootTest
@Import(MockMvcHalResourceLoaderConfiguration.class)
class InternalConsumerIT extends AbstractCompanyApiIT {

  /** This will inject the {@link CompanyApiController} instance as it's implementing that public interface */
  @Autowired
  private CompanyApi api;

  @Override
  protected CompanyApi getApiImplementionOrClientProxy() {
    return api;
  }

  @Test
  void will_fail_if_withClientPreferences_is_called() {

    // calling any other method than createLink on the CompanyApi instance will fail,
    // because this method is only meant for external consumers accessing the API through HTTP
    Throwable ex = catchThrowable(() -> api.withClientPreferences(true, true, false).getEmployees());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class);
  }
}
