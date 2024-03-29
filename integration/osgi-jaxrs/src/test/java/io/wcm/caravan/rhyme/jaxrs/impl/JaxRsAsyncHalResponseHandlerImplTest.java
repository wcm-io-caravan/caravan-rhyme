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
package io.wcm.caravan.rhyme.jaxrs.impl;

import static org.apache.http.HttpStatus.SC_NOT_IMPLEMENTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.time.Duration;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.jaxrs.api.JaxRsAsyncHalResponseRenderer;
import io.wcm.caravan.rhyme.jaxrs.impl.docs.RhymeDocsOsgiBundleSupport;

@ExtendWith(OsgiContextExtension.class)
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings("UWF_NULL_FIELD")
class JaxRsAsyncHalResponseHandlerImplTest {

  private static final String REQUEST_URL = "/request/url";

  private final OsgiContext context = new OsgiContext();

  private JaxRsAsyncHalResponseRenderer handler;

  private RequestMetricsCollector metrics = RequestMetricsCollector.create();

  @Mock
  private UriInfo uriInfo;

  @Mock
  private AsyncResponse asyncResponse;

  @BeforeEach
  void setUp() {
    lenient().when(uriInfo.getRequestUri()).thenReturn(URI.create(REQUEST_URL));

    context.registerInjectActivateService(new RhymeDocsOsgiBundleSupport());

    handler = context.registerInjectActivateService(new JaxRsAsyncHalResponseHandlerImpl());
  }

  private Response verifyResumeHasBeenCalled() {
    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    verify(asyncResponse).resume(captor.capture());
    return captor.getValue();
  }

  private void verifyResumeHasBeenCalledWithVndErrorResource(int statusCode, RuntimeException ex) {
    Response response = verifyResumeHasBeenCalled();

    assertThat(response.getStatus()).isEqualTo(statusCode);
    assertThat(response.getMediaType()).isEqualTo(MediaType.valueOf(VndErrorResponseRenderer.CONTENT_TYPE));
    assertThat(response.getEntity()).isInstanceOf(HalResource.class);

    HalResource hal = (HalResource)response.getEntity();
    assertThat(hal.getModel().path("message").asText()).isEqualTo(ex.getMessage());
    assertThat(hal.getModel().path("class").asText()).isEqualTo(ex.getClass().getName());
  }

  private Throwable verifyResumeHasBeenCalledWithFatalError() {
    ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
    verify(asyncResponse).resume(captor.capture());
    return captor.getValue();
  }

  @HalApiInterface
  public interface LinkableTestResource extends LinkableResource {
    // no additional methods required for test
  }

  @Test
  void respondWith_should_handle_successful_rendering() {

    LinkableResource resourceImpl = new LinkableTestResource() {

      @Override
      public Link createLink() {
        return new Link(REQUEST_URL);
      }
    };

    handler.respondWith(resourceImpl, uriInfo, asyncResponse, metrics);

    Response response = verifyResumeHasBeenCalled();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getMediaType()).isEqualTo(new MediaType("application", "hal+json"));
    assertThat(response.getEntity()).isInstanceOf(HalResource.class);
  }

  @Test
  void respondWith_should_sets_cache_control_header() {

    LinkableResource resourceImpl = new LinkableTestResource() {

      @Override
      public Link createLink() {
        return new Link(REQUEST_URL);
      }
    };

    metrics.setResponseMaxAge(Duration.ofSeconds(123));

    handler.respondWith(resourceImpl, uriInfo, asyncResponse, metrics);

    Response response = verifyResumeHasBeenCalled();

    assertThat(response.getHeaders()).containsKey("cache-control");
    assertThat(response.getHeaders().get("cache-control")).containsExactly(CacheControl.valueOf("no-transform, max-age=123"));
  }

  @Test
  void respondWith_should_render_runtime_exceptions_with_vnderror_resource() {

    HalApiDeveloperException ex = new HalApiDeveloperException("Not implemented!");

    LinkableResource resourceImpl = new LinkableTestResource() {

      @Override
      public Link createLink() {
        throw ex;
      }
    };

    handler.respondWith(resourceImpl, uriInfo, asyncResponse, metrics);

    verifyResumeHasBeenCalledWithVndErrorResource(500, ex);
  }

  @Test
  void respondWith_should_extract_status_code_from_jaxrs_exceptions() {

    WebApplicationException ex = new WebApplicationException("Not implemented!", SC_NOT_IMPLEMENTED);

    LinkableResource resourceImpl = new LinkableTestResource() {

      @Override
      public Link createLink() {
        throw ex;
      }
    };

    handler.respondWith(resourceImpl, uriInfo, asyncResponse, metrics);

    verifyResumeHasBeenCalledWithVndErrorResource(SC_NOT_IMPLEMENTED, ex);
  }

  @Test
  void respondWith_should_resume_with_fatal_error_thrown_in_resource() {

    Error error = new Error("Fatal Error!");

    LinkableResource resourceImpl = new LinkableTestResource() {

      @Override
      public Link createLink() {
        throw error;
      }
    };

    handler.respondWith(resourceImpl, uriInfo, asyncResponse, metrics);

    Throwable t = verifyResumeHasBeenCalledWithFatalError();

    assertThat(t).isNotNull();
  }

  @Test
  void respondWith_should_resume_with_fatal_error_thrown_in_handler() {

    NotImplementedException expectedEx = new NotImplementedException("request URI not available");
    Mockito.when(uriInfo.getRequestUri()).thenThrow(expectedEx);

    LinkableResource resourceImpl = new LinkableTestResource() {

      @Override
      public Link createLink() {
        return new Link(REQUEST_URL);
      }
    };

    handler.respondWith(resourceImpl, uriInfo, asyncResponse, metrics);

    Throwable t = verifyResumeHasBeenCalledWithFatalError();

    assertThat(t).isSameAs(expectedEx);
  }


  @Test
  void respondWithError_should_extract_status_code_from_jaxrs_exceptions() {

    WebApplicationException ex = new WebApplicationException("Not implemented!", SC_NOT_IMPLEMENTED);

    handler.respondWithError(ex, uriInfo, asyncResponse, metrics);

    verifyResumeHasBeenCalledWithVndErrorResource(SC_NOT_IMPLEMENTED, ex);
  }

  @Test
  void respondWithError_should_resume_with_fatal_error_thrown_in_handler() {

    NotImplementedException expectedEx = new NotImplementedException("request URI not available");
    Mockito.when(uriInfo.getRequestUri()).thenThrow(expectedEx);

    handler.respondWithError(new RuntimeException("foo"), uriInfo, asyncResponse, metrics);

    Throwable t = verifyResumeHasBeenCalledWithFatalError();

    assertThat(t).isSameAs(expectedEx);
  }

  @Test
  void respondWithError_should_handle_null_uris() {

    WebApplicationException ex = new WebApplicationException("Not implemented!", SC_NOT_IMPLEMENTED);

    handler.respondWithError(ex, null, asyncResponse, metrics);

    verifyResumeHasBeenCalledWithVndErrorResource(SC_NOT_IMPLEMENTED, ex);
  }
}
