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
import static io.wcm.caravan.reha.impl.renderer.AsyncHalResourceRendererTestUtil.render;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.resources.EmbeddableResource;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.testing.TestResource;
import io.wcm.caravan.reha.testing.TestState;

public class RenderEmbeddedResourceTest {


  @HalApiInterface
  public interface TestResourceWithObservableEmbedded {

    @RelatedResource(relation = ITEM)
    Observable<TestResource> getItems();
  }

  static class EmbeddedTestResource implements TestResource, EmbeddableResource {

    protected final TestState state;

    EmbeddedTestResource(TestState state) {
      this.state = state;
    }

    @Override
    public Maybe<TestState> getState() {
      return Maybe.just(state);
    }

    @Override
    public boolean isEmbedded() {
      return true;
    }
  }

  @Test
  public void multiple_embedded_resources_should_be_rendered_in_original_order() {

    List<TestState> states = Observable.range(0, 10)
        .map(i -> new TestState(i))
        .toList().blockingGet();

    TestResourceWithObservableEmbedded resourceImpl = new TestResourceWithObservableEmbedded() {

      @Override
      public Observable<TestResource> getItems() {

        return Observable.fromIterable(states)
            .map(EmbeddedTestResource::new);
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
      return true;
    }

    @Override
    public Link createLink() {
      return new Link("/" + state.number);
    }

  }

  @Test
  public void embeddable_resources_should_also_be_linked_by_default() {

    List<TestState> states = Observable.range(0, 10)
        .map(i -> new TestState(i))
        .toList().blockingGet();

    TestResourceWithObservableEmbedded resourceImpl = new TestResourceWithObservableEmbedded() {

      @Override
      public Observable<TestResource> getItems() {

        return Observable.fromIterable(states)
            .map(LinkedEmbeddableTestResource::new);
      }
    };

    HalResource hal = render(resourceImpl);
    assertThat(hal.getEmbedded(ITEM)).hasSameSizeAs(states);
    assertThat(hal.getLinks(ITEM)).hasSameSizeAs(states);
  }

  @Test
  public void embeddable_resources_should_only_be_linked__if_isEmbedded_returns_false() {

    List<TestState> states = Observable.range(0, 10)
        .map(i -> new TestState(i))
        .toList().blockingGet();

    TestResourceWithObservableEmbedded resourceImpl = new TestResourceWithObservableEmbedded() {

      @Override
      public Observable<TestResource> getItems() {

        return Observable.fromIterable(states)
            .map(state -> new LinkedEmbeddableTestResource(state) {

              @Override
              public boolean isEmbedded() {
                return false;
              }

            });
      }
    };

    HalResource hal = render(resourceImpl);
    assertThat(hal.getEmbedded(ITEM)).isEmpty();
    assertThat(hal.getLinks(ITEM)).hasSameSizeAs(states);
  }


  @Test
  public void embeddable_resources_should_only_be_embedded_if_isLinkedWhenEmbedded_returns_false() {

    List<TestState> states = Observable.range(0, 10)
        .map(i -> new TestState(i))
        .toList().blockingGet();

    TestResourceWithObservableEmbedded resourceImpl = new TestResourceWithObservableEmbedded() {

      @Override
      public Observable<TestResource> getItems() {

        return Observable.fromIterable(states)
            .map(state -> new LinkedEmbeddableTestResource(state) {

              @Override
              public boolean isLinkedWhenEmbedded() {
                return false;
              }

            });
      }
    };

    HalResource hal = render(resourceImpl);
    assertThat(hal.getEmbedded(ITEM)).hasSameSizeAs(states);
    assertThat(hal.getLinks(ITEM)).isEmpty();
  }
}
