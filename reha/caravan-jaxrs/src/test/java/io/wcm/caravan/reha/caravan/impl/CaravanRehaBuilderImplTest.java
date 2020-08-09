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
package io.wcm.caravan.reha.caravan.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.net.URI;
import java.time.Duration;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.io.http.CaravanHttpClient;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.caravan.api.CaravanReha;
import io.wcm.caravan.reha.caravan.api.CaravanRehaBuilder;
import io.wcm.caravan.reha.jaxrs.impl.JaxRsAsyncHalResponseHandlerImpl;

@ExtendWith(OsgiContextExtension.class)
@ExtendWith(MockitoExtension.class)
public class CaravanRehaBuilderImplTest {

  private static final String REQUEST_URI = "/";

  private final OsgiContext context = new OsgiContext();

  @Mock
  private CaravanHttpClient httpClient;

  @Mock
  private UriInfo uriInfo;

  @Mock
  private AsyncResponse asyncResponse;

  private CaravanRehaBuilder rehaBuilder;

  @BeforeEach
  void setUp() {

    Mockito.lenient().when(uriInfo.getRequestUri()).thenReturn(URI.create(REQUEST_URI));

    context.registerService(CaravanHttpClient.class, httpClient);
    context.registerInjectActivateService(new JaxRsAsyncHalResponseHandlerImpl());

    context.registerInjectActivateService(new CaravanHalApiClientImpl());

    rehaBuilder = context.registerInjectActivateService(new CaravanRehaBuilderImpl());
  }

  private ObjectNode mockOkHalResponse() {

    ObjectNode body = JsonNodeFactory.instance.objectNode().put("foo", "bar");

    CaravanHttpMockUtils.mockHttpResponse(httpClient, 200, body, null);

    return body;
  }

  private CaravanReha createRehaInstance() {

    return rehaBuilder.buildForRequestTo(uriInfo, asyncResponse);
  }

  @Test
  public void getEntryPoint_should_fetch_entrypoint_through_http_client() throws Exception {

    mockOkHalResponse();

    CaravanReha reha = createRehaInstance();

    LinkableTestResource resource = reha.getEntryPoint("/serviceId", REQUEST_URI, LinkableTestResource.class);

    verifyZeroInteractions(httpClient);

    assertThat(resource.getState()).isNotNull();
    verify(httpClient).execute(ArgumentMatchers.any());
  }

  @Test
  public void getUriInfo_should_return_uri_info() throws Exception {

    CaravanReha reha = createRehaInstance();

    assertThat(reha.getRequestUri()).isSameAs(uriInfo);
  }

  private Response verifyResumeHasBeenCalled() {
    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    verify(asyncResponse).resume(captor.capture());
    return captor.getValue();
  }

  @Test
  public void processRequest_should_create_context_resource_and_call_resume() throws Exception {

    CaravanReha reha = createRehaInstance();

    reha.processRequest(RequestContext::new, ResourceImpl::new);

    Response response = verifyResumeHasBeenCalled();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void setResponseMaxAge_should_limit_max_age() throws Exception {

    CaravanReha reha = createRehaInstance();

    reha.setResponseMaxAge(Duration.ofSeconds(123));

    reha.processRequest(RequestContext::new, ResourceImpl::new);

    Response response = verifyResumeHasBeenCalled();

    assertThat(response.getHeaders()).containsKey("cache-control");
    assertThat(response.getHeaders().get("cache-control")).containsExactly(CacheControl.valueOf("no-transform, max-age=123"));
  }

  static class RequestContext {

    private final CaravanReha reha;

    RequestContext(CaravanReha reha) {
      this.reha = reha;
    }
  }

  static class ResourceImpl implements LinkableTestResource {

    private final RequestContext context;

    ResourceImpl(RequestContext context) {
      this.context = context;
    }

    @Override
    public ObjectNode getState() {
      return JsonNodeFactory.instance.objectNode();
    }

    @Override
    public Link createLink() {
      return new Link(REQUEST_URI);
    }
  }

  @HalApiInterface
  public interface LinkableTestResource extends LinkableResource {

    @ResourceState
    ObjectNode getState();
  }

}
