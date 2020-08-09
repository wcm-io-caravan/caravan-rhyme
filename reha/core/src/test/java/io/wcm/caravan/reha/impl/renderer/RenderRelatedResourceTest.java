/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
package io.wcm.caravan.reha.impl.renderer;

import static io.wcm.caravan.reha.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.reha.impl.renderer.AsyncHalResourceRendererTestUtil.createSingleExternalLinkedResource;
import static io.wcm.caravan.reha.impl.renderer.AsyncHalResourceRendererTestUtil.render;
import static io.wcm.caravan.reha.testing.TestRelations.LINKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.impl.renderer.RenderLinkedResourceTest.TestResourceWithObservableLinks;
import io.wcm.caravan.reha.testing.LinkableTestResource;
import io.wcm.caravan.reha.testing.TestResource;
import io.wcm.caravan.reha.testing.TestState;

/**
 * contains tests for @RelatedResource methods that are identical for linked and embedded resources
 */
public class RenderRelatedResourceTest {

  @HalApiInterface
  public interface ResourceWithManyRelations extends LinkableResource {

    @RelatedResource(relation = "custom:ghi")
    Single<LinkableResource> getCustomGhi();

    @RelatedResource(relation = "item")
    Single<LinkableResource> getItem();

    @RelatedResource(relation = "section")
    Single<LinkableResource> getSection();

    @RelatedResource(relation = "custom:abc")
    Single<LinkableResource> getCustomAbc();

    @RelatedResource(relation = "canonical")
    Single<LinkableResource> getCanonical();

    @RelatedResource(relation = "custom:def")
    Single<LinkableResource> getCustomDef();

    @RelatedResource(relation = "alternate")
    Single<LinkableResource> getAlternate();

  }

  @Test
  public void links_should_be_ordered_alphabetical_with_standard_before_custom_relations() {

    ResourceWithManyRelations resourceImpl = new ResourceWithManyRelations() {

      @Override
      public Single<LinkableResource> getSection() {
        return createSingleExternalLinkedResource("/section");
      }

      @Override
      public Single<LinkableResource> getCustomGhi() {
        return createSingleExternalLinkedResource("/ghi");
      }

      @Override
      public Single<LinkableResource> getItem() {
        return createSingleExternalLinkedResource("/item");
      }

      @Override
      public Single<LinkableResource> getCustomAbc() {
        return createSingleExternalLinkedResource("/abc");
      }

      @Override
      public Single<LinkableResource> getCanonical() {
        return createSingleExternalLinkedResource("/canonical");
      }

      @Override
      public Single<LinkableResource> getCustomDef() {
        return createSingleExternalLinkedResource("/def");
      }

      @Override
      public Single<LinkableResource> getAlternate() {
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
  public void should_throw_runtime_exception_if_RelatedResource_method_throws_exception() {

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

    @RelatedResource(relation = LINKED)
    Maybe<TestState> getLinked();
  }

  @Test
  public void should_throw_exception_if_RelatedResource_return_type_does_not_emit_HalApiInterface() {

    TestResourceWithInvalidEmissionType resourceImpl = new TestResourceWithInvalidEmissionType() {

      @Override
      public Maybe<TestState> getLinked() {
        return Maybe.empty();
      }
    };

    Throwable ex = catchThrowable(
        () -> render(resourceImpl));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageEndingWith(
            "returns Maybe<TestState>, but it must return an interface annotated with the @HalApiInterface annotation "
                + "(or a supported generic type that provides such instances, e.g. Observable)");
  }

  @HalApiInterface
  public interface TestResourceWithExtendedType {

    @RelatedResource(relation = LINKED)
    Maybe<ExtendedLinkableTestResource> getLinked();

    interface ExtendedLinkableTestResource extends LinkableTestResource {
      // we just want to test that the extended interface can also be used in method signatures,
      // even though it does not have a HalApiAnnotation itself
    }
  }

  @Test
  public void should_allow_emission_types_that_extend_an_annotated_interface() {

    TestResourceWithExtendedType resourceImpl = new TestResourceWithExtendedType() {

      @Override
      public Maybe<ExtendedLinkableTestResource> getLinked() {
        return Maybe.empty();
      }
    };

    render(resourceImpl);
  }

  @HalApiInterface
  public interface ResourceWithInvalidRelatedMethod {

    @RelatedResource(relation = ITEM)
    Observable<TestState> getRelated();
  }

  @Test
  public void should_throw_exception_if_RelatedResource_return_type_does_not_emit_interface() {

    ResourceWithInvalidRelatedMethod resourceImpl = new ResourceWithInvalidRelatedMethod() {

      @Override
      public Observable<TestState> getRelated() {
        return Observable.empty();
      }
    };

    Throwable ex = catchThrowable(
        () -> render(resourceImpl));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageEndingWith(
            "returns Observable<TestState>, but it must return an interface annotated with the @HalApiInterface annotation "
                + "(or a supported generic type that provides such instances, e.g. Observable)");
  }

}
