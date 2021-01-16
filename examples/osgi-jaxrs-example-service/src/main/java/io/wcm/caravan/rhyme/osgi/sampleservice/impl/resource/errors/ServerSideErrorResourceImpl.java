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
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors;

import javax.ws.rs.WebApplicationException;

import io.reactivex.rxjava3.core.Maybe;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.TitledState;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceRequestContext;

public class ServerSideErrorResourceImpl implements ErrorResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final Integer statusCode;
  private final String message;
  private final Boolean withCause;

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

    return context.buildLinkTo((resource, uriInfo, response) -> resource.getServerSideError(uriInfo, response, statusCode, message, withCause))
        .setTitle("Simulate a server-side error with the given status code and message");
  }
}
