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
package io.wcm.caravan.rhyme.impl.renderer;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererTestUtil.createSingleExternalLinkedResource;
import static io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererTestUtil.render;
import static io.wcm.caravan.rhyme.testing.TestRelations.LINKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.impl.renderer.RenderLinkedResourceTest.TestResourceWithObservableLinks;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;
import io.wcm.caravan.rhyme.testing.TestResource;
import io.wcm.caravan.rhyme.testing.TestState;

/**
 * contains tests for @RelatedResource methods that are identical for linked and embedded resources
 */
class RenderRelatedResourceTest {

  @HalApiInterface
  public interface ResourceWithManyRelations extends LinkableResource {

    @Related("custom:ghi")
    Single<Link> getCustomGhi();

    @Related("item")
    Single<Link> getItem();

    @Related("section")
    Single<Link> getSection();

    @Related("custom:abc")
    Single<Link> getCustomAbc();

    @Related("canonical")
    Single<Link> getCanonical();

    @Related("custom:def")
    Single<Link> getCustomDef();

    @Related("alternate")
    Single<Link> getAlternate();

  }

  @Test
  void links_should_be_ordered_alphabetical_with_standard_before_custom_relations() {

    ResourceWithManyRelations resourceImpl = new ResourceWithManyRelations() {

      @Override
      public Single<Link> getSection() {
        return createSingleExternalLinkedResource("/section");
      }

      @Override
      public Single<Link> getCustomGhi() {
        return createSingleExternalLinkedResource("/ghi");
      }

      @Override
      public Single<Link> getItem() {
        return createSingleExternalLinkedResource("/item");
      }

      @Override
      public Single<Link> getCustomAbc() {
        return createSingleExternalLinkedResource("/abc");
      }

      @Override
      public Single<Link> getCanonical() {
        return createSingleExternalLinkedResource("/canonical");
      }

      @Override
      public Single<Link> getCustomDef() {
        return createSingleExternalLinkedResource("/def");
      }

      @Override
      public Single<Link> getAlternate() {
        return createSingleExternalLinkedResource("/alternate");
      }

      @Override
      public Link createLink() {
        return new Link("/");
      }


    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getLinks().keys()).containsExactly("self", "alternate", "canonical", "item", "section", "custom:abc", "custom:def", "custom:ghi");
  }

  @Test
  void should_throw_runtime_exception_if_RelatedResource_method_throws_exception() {

    TestResourceWithObservableLinks resourceImpl = new TestResourceWithObservableLinks() {

      @Override
      public Observable<TestResource> getLinked() {
        throw new NotImplementedException("not implemented");
      }
    };

    Throwable ex = catchThrowable(() -> render(resourceImpl));

    assertThat(ex).isInstanceOf(NotImplementedException.class);
  }

  @HalApiInterface
  public interface TestResourceWithInvalidEmissionType {

    @Related(LINKED)
    Maybe<TestState> getLinked();
  }

  @Test
  void should_throw_exception_if_RelatedResource_return_type_does_not_emit_HalApiInterface() {

    TestResourceWithInvalidEmissionType resourceImpl = new TestResourceWithInvalidEmissionType() {

      @Override
      public Maybe<TestState> getLinked() {
        return Maybe.empty();
      }
    };

    Throwable ex = catchThrowable(
        () -> render(resourceImpl));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageContaining("it must return a Link or an interface annotated with the @HalApiInterface annotation");
  }

  @HalApiInterface
  public interface TestResourceWithExtendedType {

    @Related(LINKED)
    Maybe<ExtendedLinkableTestResource> getLinked();

    interface ExtendedLinkableTestResource extends LinkableTestResource {
      // we just want to test that the extended interface can also be used in method signatures,
      // even though it does not have a HalApiAnnotation itself
    }
  }

  @Test
  void should_allow_emission_types_that_extend_an_annotated_interface() {

    TestResourceWithExtendedType resourceImpl = new TestResourceWithExtendedType() {

      @Override
      public Maybe<ExtendedLinkableTestResource> getLinked() {
        return Maybe.just(new ExtendedLinkableTestResource() {

          @Override
          public Link createLink() {
            return new Link("/foo");
          }
        });
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getLink(LINKED))
        .isNotNull();
  }

  @HalApiInterface
  public interface ResourceWithInvalidRelatedMethod {

    @Related(ITEM)
    Observable<TestState> getRelated();
  }

  @Test
  void should_throw_exception_if_RelatedResource_return_type_does_not_emit_interface() {

    ResourceWithInvalidRelatedMethod resourceImpl = new ResourceWithInvalidRelatedMethod() {

      @Override
      public Observable<TestState> getRelated() {
        return Observable.empty();
      }
    };

    Throwable ex = catchThrowable(
        () -> render(resourceImpl));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageContaining("it must return a Link or an interface annotated with the @HalApiInterface annotation");
  }

  @Test
  void should_throw_exception_if_server_impls_are_neither_linkable_or_embeddable() {

    TestResourceWithObservableLinks resourceImpl = new TestResourceWithObservableLinks() {

      @Override
      public Observable<TestResource> getLinked() {
        return Observable.just(new TestResource() {

          @Override
          public Maybe<TestState> getState() {
            return Maybe.just(new TestState("foo"));
          }

        });
      }
    };

    Throwable ex = catchThrowable(
        () -> render(resourceImpl));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("Your server side resource implementation classes must implement either EmbeddableResource or LinkableResource.");
  }

  @Test
  void should_support_plain_external_links() {

    ResourceWithCustomLink resourceImpl = new ResourceWithCustomLink() {

      @Override
      public Link createLink() {
        return new Link("/foo");
      }

      @Override
      public Single<Link> getCustomExternal() {
        return Single.just(new Link("/bar"));
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getLink("item"))
        .isNotNull()
        .extracting(Link::getHref)
        .isEqualTo("/bar");
  }

  interface CustomLinkableResource extends LinkableResource {
    // no additional methods required, as the test just verifies that custom subtypes are accepted
  }

  @HalApiInterface
  public interface ResourceWithCustomLink extends LinkableResource {

    @Related("item")
    Single<Link> getCustomExternal();
  }
}
