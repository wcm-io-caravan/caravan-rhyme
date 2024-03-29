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
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors;

import static java.lang.Boolean.TRUE;

import javax.ws.rs.WebApplicationException;

import io.reactivex.rxjava3.core.Maybe;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorParameters;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceRequestContext;

public class ServerSideErrorResourceImpl implements ErrorResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final ErrorParametersBean parameters;

  public ServerSideErrorResourceImpl(ExampleServiceRequestContext context, ErrorParameters parameters) {
    this.context = context;
    this.parameters = ErrorParametersBean.clone(parameters);
  }

  @Override
  public Maybe<String> getTitle() {

    Exception exception;
    if (TRUE.equals(parameters.getWrapException())) {
      exception = new WebApplicationException(new RuntimeException(parameters.getMessage()), parameters.getStatusCode());
    }
    else {
      exception = new WebApplicationException(parameters.getMessage(), parameters.getStatusCode());
    }

    return Maybe.error(exception);
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo((resource, uriInfo, response) -> resource.getServerSideError(uriInfo, response, parameters))
        .setTitle("Simulate a server-side error with the given status code and message");
  }
}
