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
package io.wcm.caravan.rhyme.spring.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.relations.VndErrorRelations;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.spring.impl.LinkableResourceMessageConverterTest.MinimalTestResourceImpl;


@ExtendWith(MockitoExtension.class)
public class SpringRhymeImplTest {

  private static final String REQUEST_PATH = "/foo";
  private static final String REQUEST_QUERY = "bar=123";

  @Mock
  private HttpServletRequest request;
  @Mock
  private HalResourceLoader resourceLoader;
  @Mock
  private SpringRhymeDocsIntegration rhymeDocs;

  private SpringRhymeImpl rhyme;

  @BeforeEach
  void setUp() {

    when(request.getRequestURL())
        .thenReturn(new StringBuffer(REQUEST_PATH));

    when(request.getQueryString())
        .thenReturn(REQUEST_QUERY);

    rhyme = new SpringRhymeImpl(request, resourceLoader, rhymeDocs);
  }

  @HalApiInterface
  public interface MinimalTestResource extends LinkableResource {

    @ResourceState
    ObjectNode getState();
  }

  @Test
  void getRemoteResource_should_use_HalResourceLoader() {

    String resourceUri = "/bar";

    when(resourceLoader.getHalResource(resourceUri))
        .thenReturn(Single.just(new HalResponse().withStatus(200).withBody(new HalResource())));

    MinimalTestResource resource = rhyme.getRemoteResource(resourceUri, MinimalTestResource.class);

    ObjectNode state = resource.getState();

    assertThat(state).isNotNull();

    verify(resourceLoader).getHalResource(resourceUri);
  }

  @Test
  void renderVndErrorResponse_should_use_request_url() {

    ResponseEntity<JsonNode> errorEntity = rhyme.renderVndErrorResponse(new RuntimeException());

    HalResource hal = new HalResource(errorEntity.getBody());

    Link aboutLink = hal.getLink(VndErrorRelations.ABOUT);

    assertThat(aboutLink).isNotNull();
    assertThat(aboutLink.getHref()).isEqualTo(REQUEST_PATH + "?" + REQUEST_QUERY);
  }

  @Test
  void renderResponse_should_use_max_age() {

    rhyme.setResponseMaxAge(Duration.ofSeconds(10));

    MinimalTestResourceImpl resource = new MinimalTestResourceImpl();

    ResponseEntity<JsonNode> jsonEntity = rhyme.renderResponse(resource);

    assertThat(jsonEntity.getHeaders().getCacheControl())
        .isNotNull()
        .contains("max-age=10");
  }

  @Test
  void renderResponse_should_not_embed_metadata_by_default() {

    MinimalTestResourceImpl resource = new MinimalTestResourceImpl();

    ResponseEntity<JsonNode> jsonEntity = rhyme.renderResponse(resource);

    HalResource hal = new HalResource(jsonEntity.getBody());

    assertThat(hal.getEmbeddedResource("rhyme:metadata"))
        .isNull();
  }

  @Test
  void renderResponse_should_embed_metadata_if_toggled_by_query() {

    MinimalTestResourceImpl resource = new MinimalTestResourceImpl();

    when(request.getParameterMap())
        .thenReturn(ImmutableMap.of(RequestMetricsCollector.EMBED_RHYME_METADATA, new String[0]));

    SpringRhymeImpl rhymeWithMetadata = new SpringRhymeImpl(request, resourceLoader, rhymeDocs);

    ResponseEntity<JsonNode> jsonEntity = rhymeWithMetadata.renderResponse(resource);

    HalResource hal = new HalResource(jsonEntity.getBody());

    assertThat(hal.getEmbeddedResource("rhyme:metadata"))
        .isNotNull();
  }

  @Test
  void getCoreRhyme_should_return_the_same_core_Rhyme_instance_for_multiple_calls() {

    Rhyme coreRhyme = rhyme.getCoreRhyme();

    assertThat(coreRhyme).isNotNull();

    assertThat(rhyme.getCoreRhyme()).isSameAs(coreRhyme);
  }

}
