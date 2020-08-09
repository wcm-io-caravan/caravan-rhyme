/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.reha.impl.client;

import static io.wcm.caravan.reha.impl.client.ClientTestSupport.ENTRY_POINT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.reha.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.reha.testing.resources.TestResourceState;


public class ResourceStateTest {

  private final MockClientTestSupport client = ClientTestSupport.withMocking();

  @HalApiInterface
  interface ResourceWithSingleState {

    @ResourceState
    Single<TestResourceState> getProperties();
  }

  @Test
  public void single_resource_state_should_be_emitted() throws Exception {

    client.mockHalResponseWithState(ENTRY_POINT_URI, new TestResourceState().withText("test"));

    TestResourceState properties = client.createProxy(ResourceWithSingleState.class)
        .getProperties()
        .blockingGet();

    assertThat(properties).isNotNull();
    assertThat(properties.text).isEqualTo("test");
  }


  @HalApiInterface
  interface ResourceWithOptionalState {

    @ResourceState
    Maybe<TestResourceState> getProperties();
  }

  @Test
  public void maybe_resource_state_should_be_emitted() throws Exception {

    client.mockHalResponseWithState(ENTRY_POINT_URI, new TestResourceState().withText("test"));

    TestResourceState properties = client.createProxy(ResourceWithOptionalState.class)
        .getProperties()
        .blockingGet();

    assertThat(properties).isNotNull();
    assertThat(properties.text).isEqualTo("test");
  }

  @Test
  public void maybe_resource_state_should_be_empty_if_no_properties_are_set() throws Exception {

    client.mockHalResponseWithState(ENTRY_POINT_URI, JsonNodeFactory.instance.objectNode());

    TestResourceState properties = client.createProxy(ResourceWithOptionalState.class)
        .getProperties()
        .blockingGet();

    assertThat(properties).isNull();
  }


  @HalApiInterface
  interface ResourceWithIllegalReturnType {

    @ResourceState
    Future<TestResourceState> notSupported();
  }

  @Test
  public void should_throw_developer_exception_if_return_type_is_not_supported() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithIllegalReturnType.class).notSupported());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("The given target type")
        .hasMessageEndingWith(" is not a supported return type");
  }

}
