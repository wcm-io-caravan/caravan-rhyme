/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.hal.api.server.impl;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.api.common.EmbeddableResource;
import io.wcm.caravan.hal.api.common.LinkableResource;
import io.wcm.caravan.hal.api.server.impl.AsyncHalResourceRendererImpl;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import rx.Observable;
import rx.Single;


public class AsyncHalResourceRendererImplTest {


  static class TestRelations {

    static final String LINKED = "test:linked";

    static final String EMBEDDED = "test:embedded";
  }

  static class TestState {

    public String string;
    public Integer number;

    public TestState() {

    }

    TestState(String s) {
      this.string = s;
    }

    TestState(int number) {
      this.number = number;
    }

  }

  @HalApiInterface
  public interface TestResource {

    @ResourceState
    default Single<TestState> getState() {
      return null;
    };

    @RelatedResource(relation = TestRelations.LINKED)
    default Observable<TestResource> getLinked() {
      return Observable.empty();
    }

    @RelatedResource(relation = TestRelations.EMBEDDED)
    default Observable<TestResource> getEmbedded() {
      return Observable.empty();
    }
  }

  @HalApiInterface
  public interface TestResourceWithObservableState {

    @ResourceState
    Observable<TestState> getState();
  }

  public interface LinkableTestResource extends TestResource, LinkableResource {

  }

  public interface EmbeddableTestResource extends TestResource, EmbeddableResource {

  }

  public interface LinkableEmbeddableTestResource extends LinkableResource, EmbeddableTestResource {

  }

  @Test
  public void createResource_supports_single_resource_state() {

    TestState state = new TestState("Das ist doch nur ein Test");

    TestResource resourceImpl = new TestResource() {

      @Override
      public Single<TestState> getState() {

        return Single.just(state);
      }

    };

    HalResource hal = AsyncHalResourceRendererImpl.renderResourceBlocking(resourceImpl);

    TestState actualState = hal.adaptTo(TestState.class);
    assertEquals(state.string, actualState.string);
  }

  @Test
  public void createResource_supports_observable_resource_state() {

    TestState state = new TestState("Das ist doch nur ein Test");

    TestResourceWithObservableState resourceImpl = new TestResourceWithObservableState() {

      @Override
      public Observable<TestState> getState() {

        return Observable.just(state);
      }

    };

    HalResource hal = AsyncHalResourceRendererImpl.renderResourceBlocking(resourceImpl);

    TestState actualState = hal.adaptTo(TestState.class);
    assertEquals(state.string, actualState.string);
  }

  @Test
  public void createResource_supports_single_link() {

    Link testLink = new Link("/test/url").setTitle("Test title").setName("Test name");

    TestResource resourceImpl = new TestResource() {

      @Override
      public Observable<TestResource> getLinked() {
        return Observable.just(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return testLink;
          }

        });
      }

    };

    HalResource hal = AsyncHalResourceRendererImpl.renderResourceBlocking(resourceImpl);
    assertEquals("there should be no embedded resources", 0, hal.getEmbedded(TestRelations.EMBEDDED).size());
    assertEquals("there should be exactly one link", 1, hal.getLinks(TestRelations.LINKED).size());

    Link actualLink = hal.getLink(TestRelations.LINKED);

    assertEquals(testLink.getHref(), actualLink.getHref());
    assertEquals(testLink.getName(), actualLink.getName());
    assertEquals(testLink.getTitle(), actualLink.getTitle());
  }

  @Test
  public void createResource_supports_multiple_links_in_original_order() {

    TestResource resourceImpl = new TestResource() {

      @Override
      public Observable<TestResource> getLinked() {
        return Observable.range(0, 10)
            .map(i -> new LinkableTestResource() {

              @Override
              public Link createLink() {
                return new Link("/test/" + i).setName(Integer.toString(i));
              }

            });
      }

    };

    HalResource hal = AsyncHalResourceRendererImpl.renderResourceBlocking(resourceImpl);
    assertEquals("there should be no embedded resources", 0, hal.getEmbedded(TestRelations.EMBEDDED).size());
    assertEquals("there should be exactly ten links", 10, hal.getLinks(TestRelations.LINKED).size());

    List<Link> actualLinks = hal.getLinks(TestRelations.LINKED);

    for (int i = 0; i < actualLinks.size(); i++) {
      assertEquals(Integer.toString(i), actualLinks.get(i).getName());
    }
  }

  @Test
  public void createResource_supports_embedded_resources() {

    TestResource resourceImpl = new TestResource() {

      @Override
      public Observable<TestResource> getEmbedded() {

        return Observable.range(0, 10)
            .map(i -> new EmbeddableTestResource() {

              @Override
              public Single<TestState> getState() {
                return Single.just(new TestState(i));
              }

              @Override
              public boolean isEmbedded() {
                return true;
              }

            });
      }
    };

    HalResource hal = AsyncHalResourceRendererImpl.renderResourceBlocking(resourceImpl);
    assertEquals("there should be no linked resources", 0, hal.getLinks(TestRelations.LINKED).size());
    assertEquals("there should be exactly ten embedded resources", 10, hal.getEmbedded(TestRelations.EMBEDDED).size());

  }

  @Test
  public void createResource_respects_isEmbedded_false() {

    TestResource resourceImpl = new TestResource() {

      @Override
      public Observable<TestResource> getEmbedded() {

        return Observable.range(0, 10)
            .map(i -> new LinkableEmbeddableTestResource() {

              @Override
              public Single<TestState> getState() {
                return Single.just(new TestState(i));
              }

              @Override
              public boolean isEmbedded() {
                return false;
              }

              @Override
              public Link createLink() {
                return new Link(Integer.toString(i));
              }

            });
      }
    };

    HalResource hal = AsyncHalResourceRendererImpl.renderResourceBlocking(resourceImpl);
    assertEquals("there should be no embedded resources", 0, hal.getEmbedded(TestRelations.EMBEDDED).size());
    assertEquals("there should be exactly ten links", 10, hal.getLinks(TestRelations.EMBEDDED).size());

  }


}
