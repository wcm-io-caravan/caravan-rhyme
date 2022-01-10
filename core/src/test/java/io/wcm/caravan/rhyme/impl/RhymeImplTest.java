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

import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.INVOCATION_TIMES;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.RHYME_METADATA_RELATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.server.RhymeMetadataConfiguration;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.impl.renderer.blocking.RenderResourceStateTest.TestResourceWithRequiredState;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;
import io.wcm.caravan.rhyme.testing.TestState;
import io.wcm.caravan.rhyme.testing.resources.TestResourceTree;

public class RhymeImplTest {

  private static final String UPSTREAM_ENTRY_POINT_URI = "/";
  private static final String NON_EXISTING_PATH = "/does/not/exist";
  private static final String INCOMING_REQUEST_URI = "/incoming";

  private final TestResourceTree upstreamResourceTree = new TestResourceTree();

  // create Rhyme instance with default configuration by using the RhymeBuilder without calling any customization methods
  private final Rhyme rhyme = RhymeBuilder
      .withResourceLoader(upstreamResourceTree)
      .buildForRequestTo(INCOMING_REQUEST_URI);

  @Test
  public void getEntryPoint_should_fetch_entry_point_from_upstream_resource_loader() throws Exception {

    upstreamResourceTree.getEntryPoint().setNumber(123);

    TestResourceWithRequiredState entryPoint = rhyme.getRemoteResource(UPSTREAM_ENTRY_POINT_URI, TestResourceWithRequiredState.class);

    assertThat(entryPoint.getState()).isNotNull();
    assertThat(entryPoint.getState().number).isEqualTo(123);
  }

  @Test
  public void getEntryPoint_should_fail_if_state_of_non_existing_resource_is_requested() throws Exception {

    TestResourceWithRequiredState entryPoint = rhyme.getRemoteResource(NON_EXISTING_PATH, TestResourceWithRequiredState.class);

    HalApiClientException ex = catchThrowableOfType(entryPoint::getState, HalApiClientException.class);

    assertThat(ex.getStatusCode()).isEqualTo(404);
    assertThat(ex).hasMessageStartingWith("Failed to load an upstream resource");

    assertThat(ex).hasCauseInstanceOf(HalApiClientException.class);
    assertThat(ex.getCause()).hasMessageContaining(NON_EXISTING_PATH);
  }

  private TestState getResourceFromUnknownHost(Rhyme rhymeWithoutResourceLoader) {

    return rhymeWithoutResourceLoader
        .getRemoteResource("http://foo.bar", TestResourceWithRequiredState.class)
        .getState();
  }

  @Test
  public void withoutResourceLoader_should_use_a_default_http_implementation() throws Exception {

    @SuppressWarnings("deprecation")
    Rhyme rhymeWithoutResourceLoader = RhymeBuilder
        .withoutResourceLoader()
        .buildForRequestTo(INCOMING_REQUEST_URI);

    Throwable ex = catchThrowable(() -> getResourceFromUnknownHost(rhymeWithoutResourceLoader));

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class)
        .hasRootCauseInstanceOf(UnknownHostException.class);
  }

  @Test
  public void create_should_use_a_default_http_implementation() throws Exception {

    Rhyme rhymeWithoutResourceLoader = RhymeBuilder
        .create()
        .buildForRequestTo(INCOMING_REQUEST_URI);

    Throwable ex = catchThrowable(() -> getResourceFromUnknownHost(rhymeWithoutResourceLoader));

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class)
        .hasRootCauseInstanceOf(UnknownHostException.class);
  }

  @Test
  public void setResponseMaxAge_should_affect_maxAge_of_rendered_response() throws Exception {

    rhyme.setResponseMaxAge(Duration.ofMinutes(2));

    HalResponse response = rhyme.renderResponse(new LinkableTestResourceImpl()).blockingGet();

    assertThat(response.getMaxAge()).isEqualTo(120);
  }

  private void verifyResponse(HalResponse response) {

    assertThat(response.getContentType()).isEqualTo(HalResource.CONTENT_TYPE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getLink()).isNotNull();
    assertThat(response.getBody().getLink().getHref()).isEqualTo(INCOMING_REQUEST_URI);
    assertThat(response.getBody().hasEmbedded(RHYME_METADATA_RELATION));

    TestState properties = response.getBody().adaptTo(TestState.class);
    assertThat(properties.string).isEqualTo("foo");
  }

  @Test
  public void renderResponse_should_wait_for_delayed_state() throws Exception {

    LinkableTestResourceImpl resourceImpl = new LinkableTestResourceWithDelayedState();

    HalResponse response = rhyme.renderResponse(resourceImpl).blockingGet();

    verifyResponse(response);
  }

  @Test
  public void renderResponse_should_not_wait_for_delayed_state_to_be_emitted() throws Exception {

    LinkableTestResourceImpl resourceImpl = new LinkableTestResourceWithDelayedState();

    Single<HalResponse> response = rhyme.renderResponse(resourceImpl);

    CompletableFuture<HalResponse> future = response.toCompletionStage().toCompletableFuture();
    assertThat(future).isNotCompleted();

    verifyResponse(future.get());
  }

  @Test
  public void renderVndErrorResource_should_render_given_error() throws Exception {

    HalApiServerException ex = new HalApiServerException(403, "Permission denied");

    HalResponse response = rhyme.renderVndErrorResponse(ex);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).isEqualTo(VndErrorResponseRenderer.CONTENT_TYPE);
    assertThat(response.getMaxAge()).isNull();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().hasEmbedded(RHYME_METADATA_RELATION));
  }

  private class LinkableTestResourceImpl implements LinkableTestResource {

    @Override
    public Link createLink() {
      return new Link(INCOMING_REQUEST_URI);
    }
  }

  private final class LinkableTestResourceWithDelayedState extends LinkableTestResourceImpl {

    @Override
    public Maybe<TestState> getState() {
      return Maybe.just(new TestState("foo")).delay(30, TimeUnit.MILLISECONDS);
    }
  }

  @Test
  public void startStopwatch_should_start_a_measurement_that_shows_up_in_metadata() throws Exception {

    Rhyme rhymeWithMetadata = RhymeBuilder
        .create()
        .withMetadataConfiguration(new RhymeMetadataConfiguration() {

          @Override
          public boolean isMetadataGenerationEnabled() {
            return true;
          }

        })
        .buildForRequestTo(INCOMING_REQUEST_URI);

    MeasuringTestResource resourceImpl = new MeasuringTestResource(rhymeWithMetadata);

    HalResponse response = rhymeWithMetadata.renderResponse(resourceImpl).blockingGet();

    HalResource metadata = response.getBody().getEmbeddedResource(RHYME_METADATA_RELATION);
    assertThat(metadata).isNotNull();

    HalResource metrics = metadata.getEmbeddedResource(INVOCATION_TIMES);
    assertThat(metrics).isNotNull();

    assertThat(metrics.getModel().path("title").asText())
        .isEqualTo("Invocations of methods in class " + MeasuringTestResource.class.getSimpleName());

    JsonNode measurements = metrics.getModel().path("measurements");

    assertThat(measurements)
        .hasSize(1);

    assertThat(measurements.get(0).asText())
        .endsWith("- 1x invocation of #createLink");
  }

  @Test
  public void startStopwatch_should_not_create_metadata_with_defaut_configuration() throws Exception {

    MeasuringTestResource resourceImpl = new MeasuringTestResource(rhyme);

    HalResponse response = rhyme.renderResponse(resourceImpl).blockingGet();

    HalResource metadata = response.getBody().getEmbeddedResource(RHYME_METADATA_RELATION);

    assertThat(metadata).isNull();
  }

  static class MeasuringTestResource implements LinkableTestResource {

    private final Rhyme rhyme;

    MeasuringTestResource(Rhyme rhyme) {
      this.rhyme = rhyme;
    }

    @Override
    public Link createLink() {
      try (RequestMetricsStopwatch sw = rhyme.startStopwatch(MeasuringTestResource.class,
          () -> "invocation of #createLink")) {
        return new Link("/foo");
      }
    }
  }
}
