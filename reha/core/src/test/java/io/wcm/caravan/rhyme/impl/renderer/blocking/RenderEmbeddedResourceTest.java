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
package io.wcm.caravan.rhyme.impl.renderer.blocking;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererTestUtil.render;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.ryhme.testing.TestRelations;
import io.wcm.caravan.ryhme.testing.TestState;


/**
 * Variation of the tests in {@link io.wcm.caravan.rhyme.impl.renderer.RenderEmbeddedResourceTest}
 * for blocking HAL API interfaces (i.e. that are not using reactive return types for their methods)
 */
public class RenderEmbeddedResourceTest {

  @HalApiInterface
  public interface BlockingTestResource {

    @ResourceState
    default Optional<TestState> getState() {
      return Optional.empty();
    };

    @Related(TestRelations.LINKED)
    default List<BlockingTestResource> getLinked() {
      return Collections.emptyList();
    }

    @Related(TestRelations.EMBEDDED)
    default List<BlockingTestResource> getEmbedded() {
      return Collections.emptyList();
    }
  }

  @HalApiInterface
  public interface TestResourceWithMultipleEmbedded {

    @Related(ITEM)
    List<BlockingTestResource> getItems();
  }

  @HalApiInterface
  static class EmbeddedTestResource implements BlockingTestResource, EmbeddableResource {

    protected final TestState state;

    EmbeddedTestResource(TestState state) {
      this.state = state;
    }

    @Override
    public Optional<TestState> getState() {
      return Optional.of(state);
    }
  }

  @Test
  public void multiple_embedded_resources_should_be_rendered_in_original_order() {

    List<TestState> states = Observable.range(0, 10)
        .map(i -> new TestState(i))
        .toList().blockingGet();

    TestResourceWithMultipleEmbedded resourceImpl = new TestResourceWithMultipleEmbedded() {

      @Override
      public List<BlockingTestResource> getItems() {

        return states.stream()
            .map(EmbeddedTestResource::new)
            .collect(Collectors.toList());
      }
    };

    HalResource hal = render(resourceImpl);

    List<HalResource> actualEmbedded = hal.getEmbedded(ITEM);
    assertThat(actualEmbedded).hasSameSizeAs(states);
    for (int i = 0; i < actualEmbedded.size(); i++) {
      assertThat(actualEmbedded.get(i).adaptTo(TestState.class)).isEqualToComparingFieldByField(states.get(i));
    }

    assertThat(hal.getLinks(ITEM)).isEmpty();

  }

  static class LinkedEmbeddableTestResource extends EmbeddedTestResource implements LinkableResource {

    LinkedEmbeddableTestResource(TestState state) {
      super(state);
    }

    @Override
    public boolean isEmbedded() {
      return false;
    }

    @Override
    public Link createLink() {
      return new Link("/" + state.number);
    }

  }

  @Test
  public void embeddable_resources_should_only_be_linked_if_isEmbedded_returns_false() {

    List<TestState> states = Observable.range(0, 10)
        .map(i -> new TestState(i))
        .toList().blockingGet();

    TestResourceWithMultipleEmbedded resourceImpl = new TestResourceWithMultipleEmbedded() {

      @Override
      public List<BlockingTestResource> getItems() {

        return states.stream()
            .map(LinkedEmbeddableTestResource::new)
            .collect(Collectors.toList());
      }
    };

    HalResource hal = render(resourceImpl);
    assertThat(hal.getEmbedded(ITEM)).isEmpty();
    assertThat(hal.getLinks(ITEM)).hasSameSizeAs(states);
  }

}
