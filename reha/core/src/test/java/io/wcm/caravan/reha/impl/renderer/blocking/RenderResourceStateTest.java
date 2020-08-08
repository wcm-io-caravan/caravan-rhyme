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
package io.wcm.caravan.reha.impl.renderer.blocking;

import static io.wcm.caravan.reha.impl.renderer.AsyncHalResourceRendererTestUtil.createTestState;
import static io.wcm.caravan.reha.impl.renderer.AsyncHalResourceRendererTestUtil.render;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.client.HalApiDeveloperException;
import io.wcm.caravan.reha.testing.TestState;


/**
 * Variation of the tests in {@link io.wcm.caravan.reha.impl.renderer.RenderResourceStateTest}
 * for blocking HAL API interfaces (i.e. that are not using reactive return types for their methods)
 */
public class RenderResourceStateTest {

  @HalApiInterface
  public interface TestResourceWithOptionalState {

    @ResourceState
    Optional<TestState> getState();
  }

  @Test
  public void optional_resource_state_should_be_rendered() {

    TestState state = createTestState();

    TestResourceWithOptionalState resourceImpl = new TestResourceWithOptionalState() {

      @Override
      public Optional<TestState> getState() {

        return Optional.of(state);
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.adaptTo(TestState.class)).isEqualToComparingFieldByField(state);
  }


  @HalApiInterface
  public interface TestResourceWithRequiredState {

    @ResourceState
    TestState getState();
  }

  @Test
  public void required_resource_state_should_be_rendered() {

    TestState state = createTestState();

    TestResourceWithRequiredState resourceImpl = new TestResourceWithRequiredState() {

      @Override
      public TestState getState() {

        return state;
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.adaptTo(TestState.class)).isEqualToComparingFieldByField(state);
  }


  @Test
  public void should_throw_exception_if_ResourceState_method_returns_null() {

    TestResourceWithOptionalState resourceImpl = new TestResourceWithOptionalState() {

      @Override
      public Optional<TestState> getState() {
        return null;
      }

    };

    Throwable ex = catchThrowable(
        () -> render(resourceImpl));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("#getState must not return null");
  }

  @Test
  public void should_throw_runtime_exception_if_ResourceState_method_throws_exception() {

    NotImplementedException cause = new NotImplementedException("not implemented");

    TestResourceWithOptionalState resourceImpl = new TestResourceWithOptionalState() {

      @Override
      public Optional<TestState> getState() {
        throw cause;
      }

    };

    Throwable ex = Assertions.catchThrowable(() -> render(resourceImpl));

    assertThat(ex).isInstanceOf(NotImplementedException.class);
  }

}
