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
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource;

import java.time.Duration;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.caching.CachingExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.caching.CachingExamplesResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.CollectionExamplesResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors.ErrorsExamplesResourceImpl;

public class ExamplesEntryPointResourceImpl implements ExamplesEntryPointResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  public ExamplesEntryPointResourceImpl(ExampleServiceRequestContext context) {
    this.context = context;
  }

  @Override
  public Maybe<ObjectNode> getState() {
    context.limitMaxAge(Duration.ofSeconds(60));
    return Maybe.empty();
  }

  @Override
  public Single<CollectionExamplesResource> getCollectionExamples() {

    return Single.just(new CollectionExamplesResourceImpl(context));
  }

  @Override
  public Single<CachingExamplesResource> getCachingExamples() {

    return Single.just(new CachingExamplesResourceImpl(context));
  }

  @Override
  public Single<ErrorExamplesResource> getErrorExamples() {

    return Single.just(new ErrorsExamplesResourceImpl(context));
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo((resource, uriInfo, response) -> resource.getEntryPoint(uriInfo, response))
        .setTitle("The HAL API entry point of the " + context.getServiceId() + " service");
  }

}
