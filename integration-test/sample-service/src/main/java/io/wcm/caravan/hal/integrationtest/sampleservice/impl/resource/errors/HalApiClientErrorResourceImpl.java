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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.errors;

import static io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl.SERVICE_PATH;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.reactivex.Maybe;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.TitledState;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.errors.ErrorResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@Component(service = HalApiClientErrorResourceImpl.class, scope = ServiceScope.PROTOTYPE)
@JaxrsResource
@Path(SERVICE_PATH + "/errors/halApiClient")
public class HalApiClientErrorResourceImpl implements ErrorResource, LinkableResource {

  @Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
  private ExampleServiceRequestContext context;

  @QueryParam("statusCode")
  @DefaultValue("404")
  private Integer statusCode;

  @QueryParam("message")
  @DefaultValue("error message")
  private String message;

  @QueryParam("withCause")
  @DefaultValue("false")
  private Boolean withCause;

  public HalApiClientErrorResourceImpl() {

  }

  public HalApiClientErrorResourceImpl(ExampleServiceRequestContext context, Integer statusCode, String message, Boolean withCause) {
    this.context = context;

    this.statusCode = statusCode;
    this.message = message;
    this.withCause = withCause;
  }

  @Override
  public Maybe<TitledState> getState() {

    return context.getUpstreamEntryPoint()
        .flatMap(ExamplesEntryPointResource::getErrorExamples)
        .flatMap(examples -> examples.provokeError(statusCode, message, withCause))
        .flatMapMaybe(ErrorResource::getState);
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo(this)
        .setTitle("Trigger an error when executing a HTTP request to an upstream server");
  }

  @GET
  public void get(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {
    context.respondWith(uriInfo, this, response);
  }

}
