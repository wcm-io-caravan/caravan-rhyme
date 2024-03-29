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
package io.wcm.caravan.rhyme.impl.renderer.blocking;

import static io.wcm.caravan.rhyme.impl.renderer.AsyncHalResourceRendererTestUtil.render;
import static io.wcm.caravan.rhyme.testing.TestRelations.LINKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;
import io.wcm.caravan.rhyme.testing.TestResource;

/**
 * Variation of the tests in {@link io.wcm.caravan.rhyme.impl.renderer.RenderLinkedResourceTest}
 * for blocking HAL API interfaces (i.e. that are not using reactive return types for their methods)
 */
class RenderLinkedResourceTest {

  @HalApiInterface
  public interface TestResourceWithRequiredLink {

    @Related(LINKED)
    TestResource getLinked();
  }

  @Test
  void required_link_should_be_rendered() {

    Link testLink = new Link("/test/url").setTitle("Test title").setName("Test name");

    TestResourceWithRequiredLink resourceImpl = new TestResourceWithRequiredLink() {

      @Override
      public TestResource getLinked() {
        return new LinkableTestResource() {

          @Override
          public Link createLink() {
            return testLink;
          }

        };
      }

    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).containsExactly(testLink);
    assertThat(hal.getModel().path("_links").path(LINKED).isObject()).isTrue();
  }

  @HalApiInterface
  public interface TestResourceWithOptionalLink {

    @Related(LINKED)
    Optional<TestResource> getLinked();
  }

  @Test
  void optional_link_should_be_rendered() {

    Link testLink = new Link("/test/url").setTitle("Test title").setName("Test name");

    TestResourceWithOptionalLink resourceImpl = new TestResourceWithOptionalLink() {

      @Override
      public Optional<TestResource> getLinked() {
        return Optional.of(new LinkableTestResource() {

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
  void optional_link_should_be_ignored_if_absent() {

    TestResourceWithOptionalLink resourceImpl = new TestResourceWithOptionalLink() {

      @Override
      public Optional<TestResource> getLinked() {
        return Optional.empty();
      }

    };

    HalResource hal = render(resourceImpl);
    assertThat(hal.getEmbedded(LINKED)).isEmpty();
    assertThat(hal.getLinks(LINKED)).isEmpty();
    assertThat(hal.getModel().path("_links").path(LINKED).isMissingNode()).isTrue();
  }


  @HalApiInterface
  public interface TestResourceWithMultipleLinks {

    @Related(LINKED)
    List<TestResource> getLinked();
  }

  @Test
  void multiple_links_should_be_rendered_in_original_order() {

    List<Link> links = Observable.range(0, 10)
        .map(i -> new Link("/test/" + i).setName(Integer.toString(i)))
        .toList().blockingGet();

    TestResourceWithMultipleLinks resourceImpl = new TestResourceWithMultipleLinks() {

      @Override
      public List<TestResource> getLinked() {
        return links.stream()
            .map(link -> new LinkableTestResource() {

              @Override
              public Link createLink() {
                return link;
              }

            })
            .collect(Collectors.toList());
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
  public interface TestResourceWithRequiredExternalLink {

    @Related(LINKED)
    Link getExternal();
  }

  @Test
  void single_external_link_should_be_rendered() {

    Link externalLink = new Link("http://external.url");

    TestResourceWithRequiredExternalLink resourceImpl = new TestResourceWithRequiredExternalLink() {

      @Override
      public Link getExternal() {
        return externalLink;
      }
    };

    HalResource hal = render(resourceImpl);

    assertThat(hal.getLinks(LINKED)).containsExactly(externalLink);
    assertThat(hal.getModel().path("_links").path(LINKED).isObject()).isTrue();
  }

  @Test
  void returning_null_as_external_link_should_throw_exception() {

    TestResourceWithRequiredExternalLink resourceImpl = new TestResourceWithRequiredExternalLink() {

      @Override
      public Link getExternal() {
        return null;
      }

    };

    Throwable ex = catchThrowable(
        () -> render(resourceImpl));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("#getExternal of anonymous")
        .hasMessageContaining("must not return null");
  }

  @Test
  void returning_null_in_createLink_should_throw_exception() {

    TestResourceWithRequiredLink resourceImpl = new TestResourceWithRequiredLink() {

      @Override
      public TestResource getLinked() {
        return new LinkableTestResource() {

          @Override
          public Link createLink() {
            return null;
          }

        };
      }

    };

    Throwable ex = catchThrowable(
        () -> render(resourceImpl));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageEndingWith("returned a null value");

  }

  @HalApiInterface
  public interface TestResourceWithInvalidReturnType extends LinkableResource {

    @Related(LINKED)
    String getExternal();
  }

  @Test
  void invalid_return_type_should_throw_exception() {

    TestResourceWithInvalidReturnType resourceImpl = new TestResourceWithInvalidReturnType() {

      @Override
      public Link createLink() {
        return new Link("/foo");
      }

      @Override
      public String getExternal() {
        return "bar";
      }
    };

    Throwable ex = catchThrowable(
        () -> render(resourceImpl));

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith(
            "The method #getExternal of anonymous TestResourceWithInvalidReturnType (defined in RenderLinkedResourceTest) returns String");
  }
}
