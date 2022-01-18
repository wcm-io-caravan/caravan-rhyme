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
package io.wcm.caravan.rhyme.api.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.api.common.HalResponse;


class HalApiClientExceptionTest {

  @Test
  void deprecated_constructor_should_use_given_URI()  {

    String requestUrl = "/foo";
    HalResponse response = new HalResponse().withStatus(404);

    @SuppressWarnings("deprecation")
    HalApiClientException ex = new HalApiClientException(response, requestUrl, null);

    assertThat(ex.getRequestUrl())
        .isEqualTo(requestUrl);

    assertThat(ex.getErrorResponse().getUri())
        .isEqualTo(requestUrl);
  }

}
