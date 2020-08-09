/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.reha.impl.client;

import static io.wcm.caravan.reha.api.relations.StandardRelations.ITEM;
import static io.wcm.caravan.reha.impl.client.ClientTestSupport.ENTRY_POINT_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.NotImplementedException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.reha.api.Reha;
import io.wcm.caravan.reha.api.RehaBuilder;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.RelatedResource;
import io.wcm.caravan.reha.api.annotations.ResourceRepresentation;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.common.HalApiReturnTypeSupport;
import io.wcm.caravan.reha.api.common.HalResponse;
import io.wcm.caravan.reha.api.exceptions.HalApiClientException;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.reha.impl.client.ClientTestSupport.MockClientTestSupport;
import io.wcm.caravan.reha.testing.LinkableTestResource;
import io.wcm.caravan.reha.testing.TestState;

public class ErrorHandlingTest {

  private final MockClientTestSupport client = ClientTestSupport.withMocking();

  @HalApiInterface
  interface EntryPoint {

    @ResourceState
    Maybe<TestState> getState();

    @RelatedResource(relation = ITEM)
    Observable<LinkableTestResource> getLinked();

    @ResourceRepresentation
    Single<HalResource> asHalResource();
  }

  @HalApiInterface
  interface LinkedResource {

    @ResourceState
    Maybe<TestState> getState();
  }

  public static void assertHalApiClientExceptionIsThrownWithStatus(Integer statusCode, ThrowingCallable lambda) {

    Throwable ex = catchThrowable(lambda);

    assertThat(ex).isInstanceOfSatisfying(HalApiClientException.class,
        (hace -> assertThat(hace.getStatusCode()).isEqualTo(statusCode)))
        .hasMessageStartingWith("Failed to load an upstream resource");
  }

  @Test
  public void status_code_from_response_should_be_available_in_exception_when_calling_ResourceState_method() {

    client.mockFailedResponse(ENTRY_POINT_URI, 403);

    assertHalApiClientExceptionIsThrownWithStatus(403,
        () -> client.createProxy(EntryPoint.class)
            .getState()
            .blockingGet());
  }

  @Test
  public void status_code_from_response_should_be_available_in_exception_when_calling_RelatedResource_method() {

    client.mockFailedResponse(ENTRY_POINT_URI, 501);

    assertHalApiClientExceptionIsThrownWithStatus(501,
        () -> client.createProxy(EntryPoint.class)
            .getLinked()
            .flatMapMaybe(LinkableTestResource::getState)
            .toList().blockingGet());
  }

  @Test
  public void status_code_from_response_should_be_available_in_exception_when_calling_ResourceRepresentation_method() {

    client.mockFailedResponse(ENTRY_POINT_URI, 502);

    assertHalApiClientExceptionIsThrownWithStatus(502,
        () -> client.createProxy(EntryPoint.class)
            .asHalResource()
            .blockingGet());
  }

  @Test
  public void status_code_from_response_can_be_null_if_request_failed_with_network_issues() {

    client.mockFailedResponse(ENTRY_POINT_URI, null);

    assertHalApiClientExceptionIsThrownWithStatus(null,
        () -> client.createProxy(EntryPoint.class)
            .getState()
            .blockingGet());
  }

  @Test
  public void fails_if_json_resource_loader_returns_null() {

    client.mockResponseWithSingle(ENTRY_POINT_URI, null);

    Throwable ex = catchThrowable(
        () -> client.createProxy(EntryPoint.class)
            .getState()
            .blockingGet());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasCauseInstanceOf(NullPointerException.class)
        .hasMessageContaining("returned null or threw an exception");
  }

  @Test
  public void fails_if_json_resource_loader_throws_exception() {

    IllegalStateException cause = new IllegalStateException();

    client.mockResponseWithSupplier(ENTRY_POINT_URI, () -> {
      throw cause;
    });

    Throwable ex = catchThrowable(
        () -> client.createProxy(EntryPoint.class)
            .getState()
            .blockingGet());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasCause(cause)
        .hasMessageContaining("returned null or threw an exception");
  }

  @Test
  public void fails_if_json_resource_loader_emits_unexpected_exception() {

    IllegalStateException cause = new IllegalStateException();

    client.mockResponseWithSingle(ENTRY_POINT_URI, Single.error(cause));

    Throwable ex = catchThrowable(
        () -> client.createProxy(EntryPoint.class)
            .getState()
            .blockingGet());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class)
        .hasMessageStartingWith("An unexpected exception was emitted by")
        .hasCause(cause);
  }

  @HalApiInterface
  public interface ResourceWithCustomReturnType extends LinkableTestResource {

    @RelatedResource(relation = "foo:bar")
    Stream<LinkableTestResource> getStream();
  }

  @Test
  public void fails_if_type_support_throws_unexpected_exception() {

    NotImplementedException cause = new NotImplementedException("not implemented");

    HalApiReturnTypeSupport typeSupport = new HalApiReturnTypeSupport() {

      @Override
      public Function<? super Object, Observable<?>> convertToObservable(Class<?> sourceType) {
        throw cause;
      }

      @Override
      public <T> Function<Observable, T> convertFromObservable(Class<T> targetType) {
        throw cause;
      }
    };

    client.mockHalResponseWithState(ENTRY_POINT_URI, new TestState());

    Reha reha = RehaBuilder.withResourceLoader(client.getMockJsonLoader())
        .withReturnTypeSupport(typeSupport)
        .buildForRequestTo(ENTRY_POINT_URI);

    ResourceWithCustomReturnType entryPoint = reha.getEntryPoint(ENTRY_POINT_URI, ResourceWithCustomReturnType.class);

    Throwable ex = catchThrowable(() -> entryPoint.getStream());

    assertThat(ex).isInstanceOf(RuntimeException.class)
        .hasMessageStartingWith("The invocation of ResourceWithCustomReturnType#getStream() has failed with an unexpected exception")
        .hasCause(cause);
  }

  @Test
  public void retry_operator_works_() {

    HalResponse response = new HalResponse()
        .withStatus(200)
        .withBody(new HalResource(new TestState()));

    AtomicInteger attempts = new AtomicInteger();

    Single<HalResponse> singleThatEmitsOnThirdCall = Single.fromCallable(() -> {
      int attempt = attempts.incrementAndGet();
      if (attempt == 4) {
        return response;
      }
      throw new HalApiClientException("It's not yet there", 404, ENTRY_POINT_URI, null);
    });

    client.mockResponseWithSingle(ENTRY_POINT_URI, singleThatEmitsOnThirdCall);

    TestState state = client.createProxy(EntryPoint.class)
        .getState()
        .retry(3)
        .blockingGet();

    assertThat(state).isNotNull();

    verify(client.getMockJsonLoader()).loadJsonResource(ENTRY_POINT_URI);
  }

  interface EntryPointWithoutAnnotation {

    @ResourceState
    Maybe<TestState> getState();

    @RelatedResource(relation = ITEM)
    Observable<LinkedResource> getLinked();
  }

  @Test
  public void should_throw_developer_exception_if_HalApiAnnotation_is_missing() {

    Throwable ex = catchThrowable(
        () -> client.createProxy(EntryPointWithoutAnnotation.class).getState().blockingGet());

    assertThat(ex).isInstanceOf(HalApiDeveloperException.class).hasMessageEndingWith("does not have a @HalApiInterface annotation.");
  }
}
