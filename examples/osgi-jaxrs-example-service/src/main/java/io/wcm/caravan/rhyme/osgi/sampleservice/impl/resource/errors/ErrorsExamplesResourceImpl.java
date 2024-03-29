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

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorParameters;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceRequestContext;

public class ErrorsExamplesResourceImpl implements ErrorExamplesResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  public ErrorsExamplesResourceImpl(ExampleServiceRequestContext context) {
    this.context = context;
  }

  @Override
  public Single<ErrorResource> simulateErrorOnServer(ErrorParameters parameters) {

    return Single.just(new ServerSideErrorResourceImpl(context, parameters));
  }

  @Override
  public Single<ErrorResource> testClientErrorHandling(ErrorParameters parameters) {

    return Single.just(new HalApiClientErrorResourceImpl(context, parameters));
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo((resource, uriInfo, response) -> resource.getErrorsExamples(uriInfo, response))
        .setTitle("Examples for error handling");
  }

}
