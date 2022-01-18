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
package io.wcm.caravan.rhyme.api.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;
import io.wcm.caravan.rhyme.testing.TestState;


class ResourceConversionsTest {

  class LinkableTestResourceImpl implements LinkableTestResource {

    @Override
    public Link createLink() {
      return new Link("/foo");
    }

  }

  @Test
  void asEmbeddedResource_should_return_proxy_implementing_EmbeddableResource()  {

    LinkableTestResource impl = new LinkableTestResourceImpl();

    assertThat(impl)
        .isNotInstanceOf(EmbeddableResource.class);

    LinkableTestResource proxy = ResourceConversions.asEmbeddedResource(impl);

    assertThat(proxy)
        .isInstanceOf(EmbeddableResource.class)
        .isInstanceOf(LinkableResource.class)
        .isInstanceOf(LinkableTestResource.class);
  }

  @Test
  void asEmbeddedResource_isEmbedded_returns_true()  {

    LinkableTestResource impl = new LinkableTestResourceImpl();

    LinkableTestResource proxy = ResourceConversions.asEmbeddedResource(impl);

    assertThat(((EmbeddableResource)proxy).isEmbedded())
        .isTrue();
  }

  @Test
  void asEmbeddedResource_isLinkedWhenEmbedded_returns_true()  {

    LinkableTestResource impl = new LinkableTestResourceImpl();

    LinkableTestResource proxy = ResourceConversions.asEmbeddedResource(impl);

    assertThat(((EmbeddableResource)proxy).isLinkedWhenEmbedded())
        .isTrue();
  }

  @Test
  void asEmbeddedResource_should_forward_other_method_calls_to_impl()  {

    LinkableTestResource impl = new LinkableTestResourceImpl();

    LinkableTestResource proxy = ResourceConversions.asEmbeddedResource(impl);

    assertThat(proxy.createLink())
        .isNotNull()
        .hasFieldOrPropertyWithValue("href", "/foo");
  }

  @Test
  void asEmbeddedResource_should_handle_exceptions()  {

    LinkableTestResource impl = new LinkableTestResourceImpl() {

      @Override
      public Maybe<TestState> getState() {
        throw new HalApiDeveloperException("Something has failed");
      }

    };

    LinkableTestResource proxy = ResourceConversions.asEmbeddedResource(impl);

    Throwable ex = Assertions.catchThrowable(() -> proxy.getState());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessage("Something has failed");
  }

  @Test
  void asEmbeddedResource_should_handle_errors()  {

    LinkableTestResource impl = new LinkableTestResourceImpl() {

      @Override
      public Maybe<TestState> getState() {
        throw new Error("Something has seriously failed");
      }

    };

    LinkableTestResource proxy = ResourceConversions.asEmbeddedResource(impl);

    Throwable ex = Assertions.catchThrowable(() -> proxy.getState());

    assertThat(ex)
        .isInstanceOf(HalApiServerException.class)
        .hasMessageStartingWith("Failed to invoke method getState on proxy");
  }

  @Test
  void asEmbeddedResourceWithoutLink_isEmbedded_returns_true()  {

    LinkableTestResource impl = new LinkableTestResourceImpl();

    LinkableTestResource proxy = ResourceConversions.asEmbeddedResource(impl);

    assertThat(((EmbeddableResource)proxy).isEmbedded())
        .isTrue();
  }

  @Test
  void asEmbeddedResourceWithoutLink_isLinkedWhenEmbedded_returns_false()  {

    LinkableTestResource impl = new LinkableTestResourceImpl();

    LinkableTestResource proxy = ResourceConversions.asEmbeddedResourceWithoutLink(impl);

    assertThat(((EmbeddableResource)proxy).isLinkedWhenEmbedded())
        .isFalse();
  }

  @Test
  void asLinkableResource_should_create_proxy_that_returns_given_link()  {

    Link link = new Link("/foo");

    LinkableTestResource proxy = ResourceConversions.asLinkableResource(link, LinkableTestResource.class);

    assertThat(proxy.createLink())
        .isSameAs(link);
  }

  @Test
  void asLinkableResource_should_create_proxy_that_implements_toString()  {

    Link link = new Link("/foo");

    LinkableTestResource proxy = ResourceConversions.asLinkableResource(link, LinkableTestResource.class);

    assertThat(proxy.toString())
        .isNotNull();
  }

  @Test
  void asLinkableResource_should_fail_if_any_other_method_is_called_on_proxy()  {

    Link link = new Link("/foo");

    LinkableTestResource proxy = ResourceConversions.asLinkableResource(link, LinkableTestResource.class);

    Throwable ex = catchThrowable(() -> proxy.getState());

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class);
  }

  @Test
  void asLinkableResource_can_be_used_for_interfaces_not_extending_LinkableResource()  {

    Link link = new Link("/foo");

    ResourceWithOddOverload proxy = ResourceConversions.asLinkableResource(link, ResourceWithOddOverload.class);

    assertThat(proxy)
        .isInstanceOf(LinkableResource.class);

    assertThat(((LinkableResource)proxy).createLink())
        .isSameAs(link);
  }

  @Test
  void asLinkableResource_should_fail_if_a_createLink_overload_is_called_on_proxy()  {

    Link link = new Link("/foo");

    ResourceWithOddOverload proxy = ResourceConversions.asLinkableResource(link, ResourceWithOddOverload.class);

    Throwable ex = catchThrowable(() -> proxy.createLink("foo"));

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class);
  }


  @HalApiInterface
  interface ResourceWithOddOverload {

    String createLink(String overload);
  }


}
