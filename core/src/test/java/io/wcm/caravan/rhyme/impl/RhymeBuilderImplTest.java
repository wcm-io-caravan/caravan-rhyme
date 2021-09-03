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
package io.wcm.caravan.rhyme.impl;

import static io.wcm.caravan.rhyme.api.relations.StandardRelations.ITEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
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
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;
import io.wcm.caravan.rhyme.testing.TestState;
import io.wcm.caravan.rhyme.testing.resources.TestResource;
import io.wcm.caravan.rhyme.testing.resources.TestResourceTree;

@ExtendWith(MockitoExtension.class)
public class RhymeBuilderImplTest {

  private static final String UPSTREAM_ENTRY_POINT_URI = "/";
  private static final String INCOMING_REQUEST_URI = "/incoming";

  private final TestResourceTree upstreamResourceTree = new TestResourceTree();

  private Rhyme createRhymeWithCustomExceptionStrategy() {

    return RhymeBuilder.create()
        .withExceptionStrategy(new CustomExceptionStrategy())
        .buildForRequestTo(INCOMING_REQUEST_URI);
  }

  @Test
  public void withExceptionStrategy_should_apply_custom_exception_strategy() {

    Rhyme rhyme = createRhymeWithCustomExceptionStrategy();

    NotImplementedException ex = new NotImplementedException("Foo");

    HalResponse response = rhyme.renderResponse(new FailingResourceImpl(ex)).blockingGet();

    assertThat(response.getStatus()).isEqualTo(501);
  }

  @Test
  public void withExceptionStrategy_should_not_disable_default_exception_strategy() {

    Rhyme rhyme = createRhymeWithCustomExceptionStrategy();

    HalApiServerException ex = new HalApiServerException(404, "Not Found");

    HalResponse response = rhyme.renderResponse(new FailingResourceImpl(ex)).blockingGet();

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

  private HalResponse renderFailingResource(Rhyme rhyme, RuntimeException ex) {
    return rhyme.renderResponse(new FailingResourceImpl(ex)).blockingGet();
  }

  @Test
  public void withExceptionStrategy_should_allow_multiple_custom_strategies() {

    Rhyme rhyme = RhymeBuilder.create()
        .withExceptionStrategy(new CustomExceptionStrategy())
        .withExceptionStrategy(new AdditionalExceptionStrategy())
        .buildForRequestTo(INCOMING_REQUEST_URI);

    HalResponse response404 = renderFailingResource(rhyme, new HalApiServerException(404, "Not Found"));
    assertThat(response404.getStatus()).isEqualTo(404);

    HalResponse response400 = renderFailingResource(rhyme, new IllegalArgumentException());
    assertThat(response400.getStatus()).isEqualTo(400);

    HalResponse response501 = renderFailingResource(rhyme, new NotImplementedException("Foo"));
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

  private Rhyme createRhymeWithSetReturnTypeSupport() {

    return RhymeBuilder.withResourceLoader(upstreamResourceTree)
        .withReturnTypeSupport(new SetSupport())
        .buildForRequestTo(INCOMING_REQUEST_URI);
  }

  @HalApiInterface
  public interface ResourceWithSetOfLinks extends LinkableResource {

    @Related(StandardRelations.ITEM)
    Set<LinkableTestResource> getLinks();
  }

  private static final class ResourcewithSetOfLinksImpl implements ResourceWithSetOfLinks {

    @Override
    public Set<LinkableTestResource> getLinks() {

      return ImmutableSet.of(new LinkableTestResource() {

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

    Rhyme rhyme = createRhymeWithSetReturnTypeSupport();

    ResourceWithSetOfLinks resourceImpl = new ResourcewithSetOfLinksImpl();

    HalResponse response = rhyme.renderResponse(resourceImpl).blockingGet();
    assertThat(response.getStatus()).isEqualTo(200);

    List<Link> links = response.getBody().getLinks(StandardRelations.ITEM);
    assertThat(links).hasSize(1);
  }

  @Test
  public void withReturnTypeSupport_should_enable_fetching_of_resources_with_custom_return_types() {

    upstreamResourceTree.createLinked(StandardRelations.ITEM);
    upstreamResourceTree.createLinked(StandardRelations.ITEM);

    Rhyme rhyme = createRhymeWithSetReturnTypeSupport();

    ResourceWithSetOfLinks entryPoint = rhyme.getRemoteResource(UPSTREAM_ENTRY_POINT_URI, ResourceWithSetOfLinks.class);

    Set<LinkableTestResource> linked = entryPoint.getLinks();

    assertThat(linked).hasSize(2);
  }

  private static final class SetSupport implements HalApiReturnTypeSupport {

    @SuppressWarnings("unchecked")
    @Override
    public <T> Function<Observable, T> convertFromObservable(Class<T> targetType) {
      if (targetType.isAssignableFrom(Set.class)) {
        return obs -> {
          List<?> list = (List<?>)obs.toList().blockingGet();
          // we cannot use TreeSet here, as #hashCode is not implemented by the proxy
          TreeSet<Object> set = Sets.newTreeSet(Ordering.natural().onResultOf(Object::toString));
          set.addAll(list);
          return (T)set;
        };
      }
      return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Function<? super Object, Observable<?>> convertToObservable(Class<?> sourceType) {
      if (Set.class.isAssignableFrom(sourceType)) {
        return o -> Observable.fromIterable((Set)o);
      }
      return null;
    }

    @Override
    public boolean isProviderOfMultiplerValues(Class<?> returnType) {
      return false;
    }

    /* disabled as long as (in version 1.1.0) there is a default implementation available
    @Override
    public boolean isProviderOfOptionalValue(Class<?> returnType) {
      return false;
    }
    */

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

  private Rhyme createRhymeWithCustomAnnotationTypeSupport() {

    return RhymeBuilder.withResourceLoader(upstreamResourceTree)
        .withAnnotationTypeSupport(new CustomAnnotationSupport())
        .buildForRequestTo(INCOMING_REQUEST_URI);
  }

  @Test
  public void withAnnotationTypeSupport_should_enable_rendering_of_resources_with_custom_annotation() {

    Rhyme rhyme = createRhymeWithCustomAnnotationTypeSupport();

    ResourceWithCustomAnnotation resourceImpl = new ResourceWithCustomAnnotationImpl()
        .withEmbedded(new TestState(123), new TestState(456));

    HalResponse response = rhyme.renderResponse(resourceImpl).blockingGet();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentType()).isEqualTo(HalResource.CONTENT_TYPE);

    assertThat(response.getBody().getEmbedded(ITEM)).hasSize(2);
  }

  @Test
  public void withAnnotationTypeSupport_should_enable_fetching_of_resources_with_custom_annotation() {

    TestResource entryPoint = upstreamResourceTree.getEntryPoint();
    entryPoint.setNumber(123);
    entryPoint.createEmbedded(ITEM).setNumber(456);

    Rhyme rhyme = createRhymeWithCustomAnnotationTypeSupport();

    ResourceWithCustomAnnotation resource = rhyme.getRemoteResource(UPSTREAM_ENTRY_POINT_URI, ResourceWithCustomAnnotation.class);

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

  @Test
  public void no_curies_are_rendered_if_withRhymeDocsSupport_is_not_called() {

    Rhyme rhyme = RhymeBuilder.create()
        .buildForRequestTo(INCOMING_REQUEST_URI);

    Link curieLink = renderResourceAndGetCuriesLink(rhyme);

    assertThat(curieLink).isNull();
  }

  @Test
  public void curies_are_rendered_if_withRhymeDocsSupport_is_called() {

    String baseUrl = "/docs/";

    RhymeDocsSupport docsSupport = mock(RhymeDocsSupport.class);

    when(docsSupport.getRhymeDocsBaseUrl())
        .thenReturn(baseUrl);

    Rhyme rhyme = RhymeBuilder.create()
        .withRhymeDocsSupport(docsSupport)
        .buildForRequestTo(INCOMING_REQUEST_URI);

    Link curieLink = renderResourceAndGetCuriesLink(rhyme);

    assertThat(curieLink).isNotNull();

    assertThat(curieLink.getHref())
        .startsWith(baseUrl);
  }

  private Link renderResourceAndGetCuriesLink(Rhyme rhyme) {

    HalResponse response = rhyme.renderResponse(new ResourceWithCustomRelationImpl()).blockingGet();

    assertThat(response.getStatus()).isEqualTo(200);

    return response.getBody().getLink("curies");
  }

  @HalApiInterface
  public interface ResourceWithCustomRelation extends LinkableResource {

    @Related("foo:bar")
    ResourceWithCustomRelation getBar();
  }

  class ResourceWithCustomRelationImpl implements ResourceWithCustomRelation {

    @Override
    public ResourceWithCustomRelation getBar() {
      return new ResourceWithCustomRelationImpl();
    }

    @Override
    public Link createLink() {
      return new Link(INCOMING_REQUEST_URI);
    }
  }


}
