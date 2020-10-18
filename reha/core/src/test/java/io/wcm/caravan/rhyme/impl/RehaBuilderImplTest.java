/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.caravan.rhyme.impl;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Reha;
import io.wcm.caravan.rhyme.api.RehaBuilder;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.relations.StandardRelations;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.api.spi.HalApiAnnotationSupport;
import io.wcm.caravan.rhyme.api.spi.HalApiReturnTypeSupport;
import io.wcm.caravan.ryhme.testing.LinkableTestResource;
import io.wcm.caravan.ryhme.testing.TestState;
import io.wcm.caravan.ryhme.testing.resources.TestResource;
import io.wcm.caravan.ryhme.testing.resources.TestResourceTree;

@ExtendWith(MockitoExtension.class)
public class RehaBuilderImplTest {

  private static final String UPSTREAM_ENTRY_POINT_URI = "/";
  private static final String INCOMING_REQUEST_URI = "/incoming";

  private final TestResourceTree upstreamResourceTree = new TestResourceTree();

  private Reha createRehaWithCustomExceptionStrategy() {

    return RehaBuilder.withoutResourceLoader()
        .withExceptionStrategy(new CustomExceptionStrategy())
        .buildForRequestTo(INCOMING_REQUEST_URI);
  }

  @Test
  public void withExceptionStrategy_should_apply_custom_exception_strategy() {

    Reha reha = createRehaWithCustomExceptionStrategy();

    NotImplementedException ex = new NotImplementedException("Foo");

    HalResponse response = reha.renderResponse(new FailingResourceImpl(ex));

    assertThat(response.getStatus()).isEqualTo(501);
  }

  @Test
  public void withExceptionStrategy_should_not_disable_default_exception_strategy() {

    Reha reha = createRehaWithCustomExceptionStrategy();

    HalApiServerException ex = new HalApiServerException(404, "Not Found");

    HalResponse response = reha.renderResponse(new FailingResourceImpl(ex));

    assertThat(response.getStatus()).isEqualTo(404);
  }


  private static final class CustomExceptionStrategy implements ExceptionStatusAndLoggingStrategy {

    @Override
    public Integer extractStatusCode(Throwable error) {
      if (error instanceof NotImplementedException) {
        return 501;
      }
      return null;
    }
  }

  @Test
  public void withExceptionStrategy_should_allow_multiple_custom_strategies() {

    Reha reha = RehaBuilder.withoutResourceLoader()
        .withExceptionStrategy(new CustomExceptionStrategy())
        .withExceptionStrategy(new AdditionalExceptionStrategy())
        .buildForRequestTo(INCOMING_REQUEST_URI);

    HalResponse response404 = reha.renderResponse(new FailingResourceImpl(new HalApiServerException(404, "Not Found")));
    assertThat(response404.getStatus()).isEqualTo(404);

    HalResponse response400 = reha.renderResponse(new FailingResourceImpl(new IllegalArgumentException()));
    assertThat(response400.getStatus()).isEqualTo(400);

    HalResponse response501 = reha.renderResponse(new FailingResourceImpl(new NotImplementedException("Foo")));
    assertThat(response501.getStatus()).isEqualTo(501);

  }

  private static final class AdditionalExceptionStrategy implements ExceptionStatusAndLoggingStrategy {

    @Override
    public Integer extractStatusCode(Throwable error) {
      if (error instanceof IllegalArgumentException) {
        return 400;
      }
      return null;
    }
  }

  private static final class FailingResourceImpl implements LinkableTestResource {

    private final RuntimeException ex;

    private FailingResourceImpl(RuntimeException ex) {
      this.ex = ex;
    }

    @Override
    public Link createLink() {
      throw this.ex;
    }
  }

  private Reha createRehaWithStreamReturnTypeSupport() {

    return RehaBuilder.withResourceLoader(upstreamResourceTree)
        .withReturnTypeSupport(new StreamSupport())
        .buildForRequestTo(INCOMING_REQUEST_URI);
  }

  @HalApiInterface
  public interface ResourceWithStreamOfLinks extends LinkableResource {

    @Related(StandardRelations.ITEM)
    Stream<LinkableTestResource> getLinks();
  }

  private static final class ResourcewithStreamOfLinksImpl implements ResourceWithStreamOfLinks {

    @Override
    public Stream<LinkableTestResource> getLinks() {
      return Stream.of(new LinkableTestResource() {

        @Override
        public Link createLink() {
          return new Link("/linked");
        }
      });
    }

    @Override
    public Link createLink() {
      return new Link(INCOMING_REQUEST_URI);
    }
  }

  @Test
  public void withReturnTypeSupport_should_enable_rendering_of_resources_with_custom_return_types() {

    Reha reha = createRehaWithStreamReturnTypeSupport();

    ResourceWithStreamOfLinks resourceImpl = new ResourcewithStreamOfLinksImpl();

    HalResponse response = reha.renderResponse(resourceImpl);
    assertThat(response.getStatus()).isEqualTo(200);

    List<Link> links = response.getBody().getLinks(StandardRelations.ITEM);
    assertThat(links).hasSize(1);
  }

  @Test
  public void withReturnTypeSupport_should_enable_fetching_of_resources_with_custom_return_types() {

    upstreamResourceTree.createLinked(StandardRelations.ITEM);
    upstreamResourceTree.createLinked(StandardRelations.ITEM);

    Reha reha = createRehaWithStreamReturnTypeSupport();

    ResourceWithStreamOfLinks entryPoint = reha.getUpstreamEntryPoint(UPSTREAM_ENTRY_POINT_URI, ResourceWithStreamOfLinks.class);

    List<LinkableTestResource> linked = entryPoint.getLinks().collect(Collectors.toList());

    assertThat(linked).hasSize(2);
  }

  private static final class StreamSupport implements HalApiReturnTypeSupport {

    @SuppressWarnings("unchecked")
    @Override
    public <T> Function<Observable, T> convertFromObservable(Class<T> targetType) {
      if (targetType.isAssignableFrom(Stream.class)) {
        return obs -> {
          List<?> list = (List<?>)obs.toList().blockingGet();
          return (T)list.stream();
        };
      }
      return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Function<? super Object, Observable<?>> convertToObservable(Class<?> sourceType) {
      if (Stream.class.isAssignableFrom(sourceType)) {
        return o -> Observable.fromStream((Stream)o);
      }
      return null;
    }

  }

  @MyApiInterface
  public interface ResourceWithCustomAnnotation extends LinkableResource {

    @ResourceState
    Optional<TestState> getState();

    @MyRelated(ITEM)
    List<ResourceWithCustomAnnotation> getEmbedded();
  }


  private final class ResourceWithCustomAnnotationImpl implements ResourceWithCustomAnnotation, EmbeddableResource {

    private final Optional<TestState> state;
    private final List<ResourceWithCustomAnnotation> embedded = new ArrayList<>();

    ResourceWithCustomAnnotationImpl() {
      this.state = Optional.empty();
    }

    ResourceWithCustomAnnotationImpl(TestState state) {
      this.state = Optional.ofNullable(state);
    }

    private ResourceWithCustomAnnotationImpl withEmbedded(TestState... embeddedStates) {
      Stream.of(embeddedStates)
          .map(ResourceWithCustomAnnotationImpl::new)
          .forEach(embedded::add);
      return this;
    }

    @Override
    public Optional<TestState> getState() {
      return state;
    }

    @Override
    public List<ResourceWithCustomAnnotation> getEmbedded() {
      return embedded;
    }

    @Override
    public Link createLink() {
      return new Link(INCOMING_REQUEST_URI);
    }

    @Override
    public boolean isEmbedded() {
      return state.isPresent();
    }

  }

  private Reha createRehaWithCustomAnnotationTypeSupport() {

    return RehaBuilder.withResourceLoader(upstreamResourceTree)
        .withAnnotationTypeSupport(new CustomAnnotationSupport())
        .buildForRequestTo(INCOMING_REQUEST_URI);
  }

  @Test
  public void withAnnotationTypeSupport_should_enable_rendering_of_resources_with_custom_annotation() {

    Reha reha = createRehaWithCustomAnnotationTypeSupport();

    ResourceWithCustomAnnotation resourceImpl = new ResourceWithCustomAnnotationImpl()
        .withEmbedded(new TestState(123), new TestState(456));

    HalResponse response = reha.renderResponse(resourceImpl);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentType()).isEqualTo(HalResource.CONTENT_TYPE);

    assertThat(response.getBody().getEmbedded(ITEM)).hasSize(2);
  }

  @Test
  public void withAnnotationTypeSupport_should_enable_fetching_of_resources_with_custom_annotation() {

    TestResource entryPoint = upstreamResourceTree.getEntryPoint();
    entryPoint.setNumber(123);
    entryPoint.createEmbedded(ITEM).setNumber(456);

    Reha reha = createRehaWithCustomAnnotationTypeSupport();

    ResourceWithCustomAnnotation resource = reha.getUpstreamEntryPoint(UPSTREAM_ENTRY_POINT_URI, ResourceWithCustomAnnotation.class);

    TestState state = resource.getState().get();
    assertThat(state).isNotNull();
    assertThat(state.number).isEqualTo(123);

    List<ResourceWithCustomAnnotation> embedded = resource.getEmbedded();
    assertThat(embedded).hasSize(1);
    assertThat(embedded.get(0).getState().get().number).isEqualTo(456);
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface MyApiInterface {
    // no additional properties required
  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface MyRelated {

    String value();
  }

  private static final class CustomAnnotationSupport implements HalApiAnnotationSupport {

    @Override
    public boolean isHalApiInterface(Class<?> interfaze) {

      return interfaze.isAnnotationPresent(MyApiInterface.class);
    }

    @Override
    public String getContentType(Class<?> halApiInterface) {
      return null;
    }

    @Override
    public boolean isResourceLinkMethod(Method method) {
      return false;
    }

    @Override
    public boolean isResourceRepresentationMethod(Method method) {
      return false;
    }

    @Override
    public boolean isRelatedResourceMethod(Method method) {

      return method.isAnnotationPresent(MyRelated.class);
    }

    @Override
    public boolean isResourceStateMethod(Method method) {
      return false;
    }

    @Override
    public String getRelation(Method method) {

      if (isRelatedResourceMethod(method)) {
        return method.getAnnotation(MyRelated.class).value();
      }

      return null;
    }

  }

}
