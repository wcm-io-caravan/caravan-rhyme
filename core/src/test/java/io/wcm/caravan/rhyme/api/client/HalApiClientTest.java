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
package io.wcm.caravan.rhyme.api.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;
import io.wcm.caravan.rhyme.testing.TestState;


public class HalApiClientTest {

  @Test
  public void create_should_use_a_default_http_client_implementation() throws Exception {

    HalApiClient client = HalApiClient.create();

    Throwable ex = catchThrowable(() -> getTestResourceFromUnknownHost(client));

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class)
        .hasRootCauseInstanceOf(UnknownHostException.class);
  }

  private TestState getTestResourceFromUnknownHost(HalApiClient client) {

    return client
        .getRemoteResource("http://foo.bar", LinkableTestResource.class)
        // just getting the remote resource won't trigger the HTTP request,
        // we actually have to call a method on the proxy and subscribe
        .getState()
        .blockingGet();

  }

}
