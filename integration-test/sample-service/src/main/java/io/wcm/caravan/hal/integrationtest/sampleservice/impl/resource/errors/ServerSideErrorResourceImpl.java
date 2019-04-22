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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.reactivex.Maybe;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.TitledState;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.errors.ErrorResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceApplication;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@Component(service = ServerSideErrorResourceImpl.class, scope = ServiceScope.PROTOTYPE)
@JaxrsResource
@JaxrsApplicationSelect(ExampleServiceApplication.SELECTOR)
@Path("/errors/serverSide")
public class ServerSideErrorResourceImpl implements ErrorResource, LinkableResource {

  @Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
  private ExampleServiceRequestContext context;

  @QueryParam("statusCode")
  private Integer statusCode;

  @QueryParam("message")
  private String message;

  @QueryParam("withCause")
  private Boolean withCause;

  public ServerSideErrorResourceImpl() {

  }

  public ServerSideErrorResourceImpl(ExampleServiceRequestContext context, Integer statusCode, String message, Boolean withCause) {
    this.context = context;

    this.statusCode = statusCode;
    this.message = message;
    this.withCause = withCause;
  }

  @Override
  public Maybe<TitledState> getState() {

    Exception exception;
    if (withCause != null && withCause.booleanValue()) {
      exception = new WebApplicationException(new RuntimeException(message), statusCode);
    }
    else {
      exception = new WebApplicationException(message, statusCode);
    }

    return Maybe.error(exception);
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo(this)
        .setTitle("Simulate a server-side error with the given status code and message");
  }

  @GET
  public void get(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {
    context.respondWith(uriInfo, this, response);
  }

}
