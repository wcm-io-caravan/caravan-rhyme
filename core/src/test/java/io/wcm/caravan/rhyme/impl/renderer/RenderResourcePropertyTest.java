/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
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

import static io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererTestUtil.createTestState;
import static io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererTestUtil.render;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.testing.TestState;

public class RenderResourcePropertyTest {

  @HalApiInterface
  public interface TestResourceWithSingleProperties {

    @ResourceProperty
    Single<String> getString();

    @ResourceProperty
    Single<Integer> getNumber();
  }

  @Test
  public void single_resource_properties_should_be_rendered() {

    TestState state = createTestState();

    TestResourceWithSingleProperties resourceImpl = new TestResourceWithSingleProperties() {

      @Override
      public Single<String> getString() {
        return Single.just(state.string);
      }

      @Override
      public Single<Integer> getNumber() {
        return Single.just(state.number);
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.adaptTo(TestState.class))
        .isEqualToComparingFieldByField(state);
  }


  @HalApiInterface
  public interface TestResourceWithMaybeProperties {

    @ResourceProperty
    Maybe<String> getString();

    @ResourceProperty
    Maybe<Integer> getNumber();
  }

  @Test
  public void maybe_resource_properties_should_be_rendered_if_value_is_available() {

    TestResourceWithMaybeProperties resourceImpl = new TestResourceWithMaybeProperties() {

      @Override
      public Maybe<String> getString() {
        return Maybe.just("foo");
      }

      @Override
      public Maybe<Integer> getNumber() {
        return Maybe.empty();
      }
    };

    JsonNode json = render(resourceImpl).getModel();

    assertThat(json.path("string").asText())
        .isEqualTo("foo");

    assertThat(json.path("number").isMissingNode())
        .isTrue();
  }


  @HalApiInterface
  public interface TestResourceWithNamedProperties {

    @ResourceProperty("foo")
    String getString();

    @ResourceProperty
    String bar();

    @ResourceProperty
    Boolean isValid();
  }

  @Test
  public void property_names_should_be_taken_from_annotation_or_derived_from_method() {

    TestResourceWithNamedProperties resourceImpl = new TestResourceWithNamedProperties() {

      @Override
      public String getString() {
        return "foo";
      }

      @Override
      public String bar() {
        return "bar";
      }

      @Override
      public Boolean isValid() {
        return true;
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getStateFieldNames())
        .containsExactlyInAnyOrder("foo", "bar", "valid");
  }

  @HalApiInterface
  public interface TestResourceWithStateAndProperties {

    @ResourceState
    TestState getState();

    @ResourceProperty
    Integer getNumber();

    @ResourceProperty
    Boolean isValid();
  }

  @Test
  public void properties_should_be_merged_with_state() {

    TestState state = createTestState();

    TestResourceWithStateAndProperties resourceImpl = new TestResourceWithStateAndProperties() {

      @Override
      public TestState getState() {
        return state;
      }

      @Override
      public Integer getNumber() {
        return 1234;
      }

      @Override
      public Boolean isValid() {
        return true;
      }
    };

    JsonNode json = render(resourceImpl).getModel();

    assertThat(json.path("string").asText())
        .isEqualTo(state.string);

    // the return value from #getNumber should overwrite the property from TestState
    assertThat(json.path("number").asInt())
        .isEqualTo(resourceImpl.getNumber());

    assertThat(json.path("valid").asBoolean())
        .isEqualTo(resourceImpl.isValid());
  }


  @HalApiInterface
  public interface TestResourceWithObservableReturnType {

    @ResourceProperty
    Observable<Integer> getNumber();
  }


  @Test
  public void should_throw_developer_exception_if_using_observable_as_return_type() {

    TestResourceWithObservableReturnType resourceImpl = new TestResourceWithObservableReturnType() {

      @Override
      public Observable<Integer> getNumber() {
        return Observable.just(123);
      }

    };

    Throwable ex = catchThrowable(() -> render(resourceImpl));

    assertThat(ex)
        .isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("@ResourceProperty cannot be used for arrays");
  }

}
