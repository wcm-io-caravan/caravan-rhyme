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

import io.reactivex.rxjava3.core.Maybe;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.TitledState;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.errors.ErrorResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.resources.LinkableResource;

public class HalApiClientErrorResourceImpl implements ErrorResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final Integer statusCode;
  private final String message;
  private final Boolean withCause;

  public HalApiClientErrorResourceImpl(ExampleServiceRequestContext context, Integer statusCode, String message, Boolean withCause) {
    this.context = context;
    this.statusCode = statusCode;
    this.message = message;
    this.withCause = withCause;
  }

  @Override
  public Maybe<TitledState> getState() {

    return context.getUpstreamEntryPoint()
        .getErrorExamples()
        .flatMap(examples -> examples.provokeError(statusCode, message, withCause))
        .flatMapMaybe(ErrorResource::getState);
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo((resource, uriInfo, response) -> resource.getHalApiClientError(uriInfo, response, statusCode, message, withCause))
        .setTitle("Trigger an error when executing a HTTP request to an upstream server");
  }
}
