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

import static io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererTestUtil.createSingleExternalLinkedResource;
import static io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererTestUtil.render;
import static io.wcm.caravan.rhyme.testing.TestRelations.LINKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;
import io.wcm.caravan.rhyme.testing.TestResource;

class RenderLinkedResourceTest {

  @HalApiInterface
  public interface TestResourceWithSingleLink {

    @Related(LINKED)
    Single<TestResource> getLinked();
  }

  @Test
  void self_link_should_be_rendered() {

    Link link = new Link("/foo/bar").setTitle("Title of the self link");

    LinkableTestResource resourceImpl = new LinkableTestResource() {

      @Override
      public Link createLink() {
        return link;
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getLink().getModel()).isEqualTo(link.getModel());
  }

  @Test
  void single_link_should_be_rendered() {

    Link testLink = new Link("/test/url").setTitle("Test title").setName("Test name");

    TestResourceWithSingleLink resourceImpl = new TestResourceWithSingleLink() {

      @Override
      public Single<TestResource> getLinked() {
        return Single.just(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return testLink;
          }

        });
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).containsExactly(testLink);
    assertThat(hal.getModel().path("_links").path(LINKED).isObject()).isTrue();
  }

  @HalApiInterface
  public interface TestResourceWithMaybeLink {

    @Related(LINKED)
    Maybe<TestResource> getLinked();
  }

  @Test
  void maybe_link_should_be_rendered() {

    Link testLink = new Link("/test/url").setTitle("Test title").setName("Test name");

    TestResourceWithMaybeLink resourceImpl = new TestResourceWithMaybeLink() {

      @Override
      public Maybe<TestResource> getLinked() {
        return Maybe.just(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return testLink;
          }

        });
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).containsExactly(testLink);
    assertThat(hal.getModel().path("_links").path(LINKED).isObject()).isTrue();
  }

  @Test
  void maybe_link_should_be_ignored_if_absent() {

    TestResourceWithMaybeLink resourceImpl = new TestResourceWithMaybeLink() {

      @Override
      public Maybe<TestResource> getLinked() {
        return Maybe.empty();
      }

    };

    HalResource hal = render(resourceImpl);
    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).isEmpty();
    assertThat(hal.getModel().path("_links").path(LINKED).isMissingNode()).isTrue();
  }

  @HalApiInterface
  public interface TestResourceWithPublisherLinks {

    @Related(LINKED)
    Publisher<TestResource> getLinked();
  }

  @Test
  void publisher_link_should_be_rendered() {

    Link testLink = new Link("/test/url").setTitle("Test title").setName("Test name");

    TestResourceWithPublisherLinks resourceImpl = new TestResourceWithPublisherLinks() {

      @Override
      public Publisher<TestResource> getLinked() {
        return Flowable.just(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return testLink;
          }

        });
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).containsExactly(testLink);
  }


  @HalApiInterface
  public interface TestResourceWithObservableLinks {

    @Related(LINKED)
    Observable<TestResource> getLinked();
  }

  @Test
  void multiple_links_should_be_rendered_in_original_order() {

    List<Link> links = Observable.range(0, 10)
        .map(i -> new Link("/test/" + i).setName(Integer.toString(i)))
        .toList().blockingGet();

    TestResourceWithObservableLinks resourceImpl = new TestResourceWithObservableLinks() {

      @Override
      public Observable<TestResource> getLinked() {
        return Observable.fromIterable(links)
            .map(link -> new LinkableTestResource() {

              @Override
              public Link createLink() {
                return link;
              }

            });
      }

    };

    HalResource hal = render(resourceImpl);
    assertThat(hal.getEmbedded(LINKED)).isEmpty();

    List<Link> actualLinks = hal.getLinks(LINKED);
    assertThat(actualLinks).hasSameSizeAs(links);
    for (int i = 0; i < actualLinks.size(); i++) {
      assertThat(actualLinks.get(i)).isEqualTo(links.get(i));
    }
  }

  @HalApiInterface
  public interface TestResourceWithSingleLinkTemplate {

    @Related(LINKED)
    Single<TestResource> getLinkedWithNumber(@TemplateVariable("number") Integer number);
  }

  @Test
  void single_link_template_should_be_rendered() {

    Link testLink = new Link("/test/{number}");

    TestResourceWithSingleLinkTemplate resourceImpl = new TestResourceWithSingleLinkTemplate() {

      @Override
      public Single<TestResource> getLinkedWithNumber(Integer number) {
        return Single.just(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return testLink;
          }

        });
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).containsExactly(testLink);
    assertThat(hal.getModel().path("_links").path(LINKED).isObject()).isTrue();
  }

  @HalApiInterface
  public interface TestResourceWithSingleExternalLink {

    @Related(LINKED)
    Single<Link> getExternal();
  }

  @Test
  void single_external_link_should_be_rendered() {

    Link externalLink = new Link("http://external.url");

    TestResourceWithSingleExternalLink resourceImpl = new TestResourceWithSingleExternalLink() {

      @Override
      public Single<Link> getExternal() {
        return createSingleExternalLinkedResource(externalLink);
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getLinks(LINKED)).containsExactly(externalLink);
    assertThat(hal.getModel().path("_links").path(LINKED).isObject()).isTrue();
  }

  @Test
  void returning_null_in_createLink_should_throw_exception() {

    TestResourceWithSingleLink resourceImpl = new TestResourceWithSingleLink() {

      @Override
      public Single<TestResource> getLinked() {
        return Single.just(new LinkableTestResource() {

          @Override
          public Link createLink() {
            return null;
          }

        });
      }

    };

    Throwable ex = catchThrowable(
        () -> render(resourceImpl));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageEndingWith("returned a null value");
  }

}
