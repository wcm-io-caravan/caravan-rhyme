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
package io.wcm.caravan.rhyme.api.server;

import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.RHYME_METADATA_RELATION;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.spi.ExceptionStatusAndLoggingStrategy;
import io.wcm.caravan.rhyme.testing.LinkableTestResource;


@ExtendWith(MockitoExtension.class)
class AsyncHalResponseRendererTest {

  private static final String REQUEST_URI = "/";

  private AsyncHalResponseRenderer createRenderer() {
    RequestMetricsCollector metrics = RequestMetricsCollector.create();
    ExceptionStatusAndLoggingStrategy strategy = new ExceptionStatusAndLoggingStrategy() {
      // we want to cover the default implementation here, so we don't override anything
    };

    return AsyncHalResponseRenderer.create(metrics, strategy);
  }

  // these are just two very basic test cases to cover the wiring in the api.server package,
  // and verify basic interaction for a successful and one error response

  @Test
  void valid_resource_should_be_rendered() {

    AsyncHalResponseRenderer renderer = createRenderer();

    HalResponse response = renderer.renderResponse(REQUEST_URI, new LinkableTestResource() {

      @Override
      public Link createLink() {
        return new Link(REQUEST_URI);
      }
    }).blockingGet();

    assertThat(response).isNotNull();
    assertThat(response.getContentType()).isEqualTo(HalResource.CONTENT_TYPE);
    assertThat(response.getBody().getLink()).isNotNull();
    assertThat(response.getBody().hasEmbedded(RHYME_METADATA_RELATION)).isTrue();
  }

  @Test
  void error_resource_should_be_rendered_if_an_exception_is_thrown() {

    AsyncHalResponseRenderer renderer = createRenderer();

    HalResponse response = renderer.renderResponse(REQUEST_URI, new LinkableTestResource() {

      @Override
      public Link createLink() {
        throw new RuntimeException("failed to create link");
      }
    }).blockingGet();

    assertThat(response).isNotNull();
    assertThat(response.getContentType()).isEqualTo(VndErrorResponseRenderer.CONTENT_TYPE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().hasEmbedded(RHYME_METADATA_RELATION)).isTrue();
  }


  @Test
  void deprecated_method_without_RhymeDocsSupport_should_not_throw_exception() {

    RequestMetricsCollector metrics = RequestMetricsCollector.create();
    ExceptionStatusAndLoggingStrategy strategy = new ExceptionStatusAndLoggingStrategy() {
      // we want to cover the default implementation here, so we don't override anything
    };

    AsyncHalResponseRenderer renderer = AsyncHalResponseRenderer.create(metrics, strategy, null, null);
    assertThat(renderer)
        .isNotNull();
  }
}
