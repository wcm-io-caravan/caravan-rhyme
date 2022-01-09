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
package io.wcm.caravan.rhyme.caravan.impl;

import static org.apache.http.HttpStatus.SC_NOT_IMPLEMENTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HttpHeaders;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.VndErrorResponseRenderer;
import io.wcm.caravan.rhyme.caravan.api.CaravanRhyme;
import io.wcm.caravan.rhyme.caravan.impl.CaravanRhymeRequestCycleImpl.CaravanRhymeImpl;
import io.wcm.caravan.rhyme.jaxrs.impl.JaxRsAsyncHalResponseHandlerImpl;
import io.wcm.caravan.rhyme.jaxrs.impl.docs.RhymeDocsOsgiBundleSupport;

@ExtendWith(OsgiContextExtension.class)
@ExtendWith(MockitoExtension.class)
public class CaravanRhymeRequestCycleImplTest {

  private static final String REQUEST_URI = "/";

  private final OsgiContext context = new OsgiContext();

  @Mock
  private CaravanHttpClient httpClient;

  @Mock
  private UriInfo uriInfo;

  @Mock
  private AsyncResponse asyncResponse;

  private CaravanRhymeRequestCycleImpl requestCycle;

  @BeforeEach
  void setUp() {

    lenient().when(uriInfo.getRequestUri())
        .thenReturn(URI.create(REQUEST_URI));

    when(uriInfo.getQueryParameters())
        .thenReturn(new MultivaluedHashMap<>());

    context.registerService(CaravanHttpClient.class, httpClient);

    context.registerInjectActivateService(new RhymeDocsOsgiBundleSupport());

    context.registerInjectActivateService(new JaxRsAsyncHalResponseHandlerImpl());

    context.registerInjectActivateService(new CaravanHalApiClientImpl());

    requestCycle = context.registerInjectActivateService(new CaravanRhymeRequestCycleImpl());
  }

  private ObjectNode mockOkHalResponse() {

    ObjectNode body = JsonNodeFactory.instance.objectNode().put("foo", "bar");

    CaravanHttpMockUtils.mockHttpResponse(httpClient, 200, body, null);

    return body;
  }

  @Test
  public void getEntryPoint_should_fetch_entrypoint_through_http_client() throws Exception {

    mockOkHalResponse();

    CaravanRhyme rhyme = requestCycle.createRhymeInstance(uriInfo);

    LinkableTestResource resource = rhyme.getRemoteResource("/serviceId", REQUEST_URI, LinkableTestResource.class);

    verifyZeroInteractions(httpClient);

    assertThat(resource.getState()).isNotNull();
    verify(httpClient).execute(ArgumentMatchers.any());
  }

  @Test
  public void getUriInfo_should_return_uri_info() throws Exception {

    CaravanRhyme rhyme = requestCycle.createRhymeInstance(uriInfo);

    assertThat(rhyme.getRequestUri()).isSameAs(uriInfo);
  }

  private Response verifyResumeHasBeenCalled() {
    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    verify(asyncResponse).resume(captor.capture());
    return captor.getValue();
  }

  @Test
  public void processRequest_should_create_context_resource_and_call_resume() throws Exception {

    requestCycle.processRequest(uriInfo, asyncResponse, RequestContext::new, ResourceImpl::new);

    Response response = verifyResumeHasBeenCalled();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo(HalResource.CONTENT_TYPE);
  }

  @Test
  public void processRequest_should_handle_exception_when_creating_resource() throws Exception {

    requestCycle.processRequest(uriInfo, asyncResponse, RequestContext::new, this::failWithNotImplemented);

    Response response = verifyResumeHasBeenCalled();

    assertThatVndErrorResponseIsRendered(response);
  }

  @Test
  public void processRequest_should_handle_exception_when_creating_context() throws Exception {

    requestCycle.processRequest(uriInfo, asyncResponse, this::failWithNotImplemented, ResourceImpl::new);

    Response response = verifyResumeHasBeenCalled();

    assertThatVndErrorResponseIsRendered(response);
  }

  @Test
  public void processRequest_should_not_include_metadata_in_response_by_default() throws Exception {

    requestCycle.processRequest(uriInfo, asyncResponse, RequestContext::new, ResourceImpl::new);

    HalResource hal = getHalResourceFromResponseBody();

    assertThat(hal.getEmbedded("rhyme:metadata"))
        .isEmpty();
  }

  @Test
  public void processRequest_should_include_metadata_if_query_param_is_set() throws Exception {

    MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
    queryParams.put(RequestMetricsCollector.EMBED_RHYME_METADATA, Collections.emptyList());

    when(uriInfo.getQueryParameters()).thenReturn(queryParams);

    requestCycle.processRequest(uriInfo, asyncResponse, RequestContext::new, ResourceImpl::new);

    HalResource hal = getHalResourceFromResponseBody();

    assertThat(hal.getEmbedded("rhyme:metadata"))
        .isNotEmpty();
  }

  private HalResource getHalResourceFromResponseBody() {

    Response response = verifyResumeHasBeenCalled();

    assertThat(response.getEntity())
        .isInstanceOf(HalResource.class);

    return (HalResource)response.getEntity();
  }

  @Test
  public void processRequest_should_handle_exception_when_rendering_state() throws Exception {

    requestCycle.processRequest(uriInfo, asyncResponse, RequestContext::new, ResourceImplFailsToRender::new);

    Response response = verifyResumeHasBeenCalled();

    assertThatVndErrorResponseIsRendered(response);
  }

  @Test
  public void processRequest_should_handle_exception_when_creatingLink() throws Exception {

    requestCycle.processRequest(uriInfo, asyncResponse, RequestContext::new, ResourceImplFailsToCreateLink::new);

    Response response = verifyResumeHasBeenCalled();

    assertThatVndErrorResponseIsRendered(response);
  }

  private void assertThatVndErrorResponseIsRendered(Response response) {

    // verify that the status code from the thrown exception is actually used in the rsponse
    assertThat(response.getStatus()).isEqualTo(SC_NOT_IMPLEMENTED);
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo(VndErrorResponseRenderer.CONTENT_TYPE);
  }


  private <T, U> U failWithNotImplemented(T parameter) {
    throw new WebApplicationException("Failed to create context with rhyme instance " + parameter, HttpStatus.SC_NOT_IMPLEMENTED);
  }

  @Test
  public void setResponseMaxAge_should_limit_max_age() throws Exception {

    requestCycle.processRequest(uriInfo, asyncResponse, rhyme -> {
      rhyme.setResponseMaxAge(Duration.ofSeconds(123));
      return new RequestContext(rhyme);
    }, ResourceImpl::new);

    Response response = verifyResumeHasBeenCalled();

    assertThat(response.getHeaders()).containsKey("cache-control");
    assertThat(response.getHeaders().get("cache-control")).containsExactly(CacheControl.valueOf("no-transform, max-age=123"));
  }

  static class RequestContext {

    private final CaravanRhyme rhyme;

    RequestContext(CaravanRhyme rhyme) {
      this.rhyme = rhyme;
      assertThat(rhyme).isInstanceOf(CaravanRhymeImpl.class);
    }
  }

  static class ResourceImpl implements LinkableTestResource {

    private final RequestContext context;

    ResourceImpl(RequestContext context) {
      this.context = context;
      assertThat(context).isNotNull();
    }

    @Override
    public ObjectNode getState() {

      return JsonNodeFactory.instance.objectNode()
          .put("foo", context.rhyme.toString());
    }

    @Override
    public Link createLink() {
      return new Link(REQUEST_URI);
    }
  }

  static class ResourceImplFailsToRender extends ResourceImpl {

    ResourceImplFailsToRender(RequestContext context) {
      super(context);
    }

    @Override
    public ObjectNode getState() {
      throw new WebApplicationException(SC_NOT_IMPLEMENTED);
    }

  }

  static class ResourceImplFailsToCreateLink extends ResourceImpl {

    ResourceImplFailsToCreateLink(RequestContext context) {
      super(context);
    }

    @Override
    public Link createLink() {
      throw new WebApplicationException(SC_NOT_IMPLEMENTED);
    }

  }

  @HalApiInterface
  public interface LinkableTestResource extends LinkableResource {

    @ResourceState
    ObjectNode getState();
  }

}
