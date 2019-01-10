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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.errors.ErrorExamplesResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.errors.ErrorResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@Path("/errors")
public class ErrorsExamplesResourceImpl implements ErrorExamplesResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  public ErrorsExamplesResourceImpl(@Context ExampleServiceRequestContext context) {
    this.context = context;
  }

  @Override
  public Single<ErrorResource> provokeError(Integer statusCode, String message, Boolean withCause) {
    return Single.just(new ServerSideErrorResourceImpl(context, statusCode, message, withCause));
  }

  @Override
  public Single<ErrorResource> provokeHttpClientError(Integer statusCode, String message, Boolean withCause) {
    return Single.just(new HalApiClientErrorResourceImpl(context, statusCode, message, withCause));
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo(this)
        .setTitle("Examples for error handling");
  }

  @GET
  public void get(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {
    context.respondWith(this, response);
  }
}
