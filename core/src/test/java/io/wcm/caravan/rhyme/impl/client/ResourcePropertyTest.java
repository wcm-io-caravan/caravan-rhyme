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
package io.wcm.caravan.rhyme.impl.client;

import static io.wcm.caravan.rhyme.impl.client.ClientTestSupport.ENTRY_POINT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.rhyme.testing.resources.TestResourceState;


public class ResourcePropertyTest {

  private final MockClientTestSupport client = ClientTestSupport.withMocking();

  @HalApiInterface
  interface ResourceWithSingleProperties {

    @ResourceProperty
    Single<String> getText();


    @ResourceProperty
    Single<Integer> getNumber();
  }

  @Test
  public void single_resource_properties_should_be_emitted() throws Exception {

    client.mockHalResponseWithState(ENTRY_POINT_URI, new TestResourceState().withText("test").withNumber(1234));

    ResourceWithSingleProperties proxy = client.createProxy(ResourceWithSingleProperties.class);

    assertThat(proxy.getText().blockingGet())
        .isEqualTo("test");

    assertThat(proxy.getNumber().blockingGet())
        .isEqualTo(1234);
  }


  @HalApiInterface
  interface ResourceWithMaybeProperties {

    @ResourceProperty
    Maybe<String> getText();


    @ResourceProperty
    Maybe<Integer> getNumber();
  }

  @Test
  public void maybe_resource_state_should_be_emitted() throws Exception {

    client.mockHalResponseWithState(ENTRY_POINT_URI, new TestResourceState().withText("test").withNumber(1234));

    ResourceWithMaybeProperties proxy = client.createProxy(ResourceWithMaybeProperties.class);

    assertThat(proxy.getText().blockingGet())
        .isEqualTo("test");

    assertThat(proxy.getNumber().blockingGet())
        .isEqualTo(1234);
  }

  @Test
  public void maybe_resource_state_should_be_empty_if_no_properties_are_set() throws Exception {

    client.mockHalResponseWithState(ENTRY_POINT_URI, JsonNodeFactory.instance.objectNode());

    ResourceWithMaybeProperties proxy = client.createProxy(ResourceWithMaybeProperties.class);

    assertThat(proxy.getText().isEmpty().blockingGet())
        .isTrue();

    assertThat(proxy.getNumber().isEmpty().blockingGet())
        .isTrue();
  }

  @HalApiInterface
  interface ResourceWithRenamedProperties {

    @ResourceProperty("text")
    String getString();


    @ResourceProperty("number")
    Integer getInteger();
  }

  @Test
  public void should_use_names_from_annotation() throws Exception {

    client.mockHalResponseWithState(ENTRY_POINT_URI, new TestResourceState().withText("test").withNumber(1234));

    ResourceWithRenamedProperties proxy = client.createProxy(ResourceWithRenamedProperties.class);

    assertThat(proxy.getString())
        .isEqualTo("test");

    assertThat(proxy.getInteger())
        .isEqualTo(1234);
  }

  @Test
  public void should_throw_developer_exception_for_null_values() throws Exception {

    client.mockHalResponseWithState(ENTRY_POINT_URI, new TestResourceState());

    ResourceWithRenamedProperties proxy = client.createProxy(ResourceWithRenamedProperties.class);

    Throwable ex = catchThrowable(() -> proxy.getString());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("The JSON property 'text' is NULL");
  }

  @Test
  public void should_throw_developer_exception_for_missing_values() throws Exception {

    client.mockHalResponseWithState(ENTRY_POINT_URI, JsonNodeFactory.instance.objectNode());

    ResourceWithRenamedProperties proxy = client.createProxy(ResourceWithRenamedProperties.class);

    Throwable ex = catchThrowable(() -> proxy.getString());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("The JSON property 'text' is MISSING");
  }

  @HalApiInterface
  interface ResourceWithListProperty {

    @ResourceProperty
    List<String> getStrings();
  }

  @Test
  public void should_throw_developer_exception_if_using_list_return_type() throws Exception {

    client.mockHalResponseWithState(ENTRY_POINT_URI, JsonNodeFactory.instance.objectNode());

    ResourceWithListProperty proxy = client.createProxy(ResourceWithListProperty.class);

    Throwable ex = catchThrowable(() -> proxy.getStrings());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("@ResourceProperty cannot be used for arrays");
  }

  @HalApiInterface
  interface ResourceWithIllegalReturnType {

    @ResourceProperty
    Future<TestResourceState> notSupported();
  }

  @Test
  public void should_throw_developer_exception_if_return_type_is_not_supported() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(ResourceWithIllegalReturnType.class).notSupported());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("The return type Future of method ResourceWithIllegalReturnType#notSupported is not supported.");
  }

}
