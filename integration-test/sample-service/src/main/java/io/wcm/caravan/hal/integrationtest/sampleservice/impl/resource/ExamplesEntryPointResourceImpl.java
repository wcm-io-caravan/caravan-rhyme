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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import io.wcm.caravan.hal.api.server.LinkableResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.CollectionExamplesResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.CollectionExamplesResourceImpl;
import io.wcm.caravan.hal.resource.Link;
import rx.Observable;

@Path("")
public class ExamplesEntryPointResourceImpl implements ExamplesEntryPointResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  public ExamplesEntryPointResourceImpl(@Context ExampleServiceRequestContext context) {
    this.context = context;
  }

  @Override
  public Observable<CollectionExamplesResource> getCollectionExamples() {
    return Observable.just(new CollectionExamplesResourceImpl(context));
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo(this)
        .setTitle("The HAL API entry point of the " + context.getContextPath() + " service");
  }

  @GET
  public void get(@Suspended AsyncResponse response) {
    context.respondWith(this, response);
  }

}
