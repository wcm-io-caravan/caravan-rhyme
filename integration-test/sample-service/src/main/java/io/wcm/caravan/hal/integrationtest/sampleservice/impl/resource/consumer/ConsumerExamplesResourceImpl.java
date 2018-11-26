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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.consumer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.CollectionExamplesResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.TitledState;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@Path("/consumer")
public class ConsumerExamplesResourceImpl implements CollectionExamplesResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  public ConsumerExamplesResourceImpl(@Context ExampleServiceRequestContext context) {
    this.context = context;
  }

  @Override
  public Single<TitledState> getState() {
    return Single.just(new TitledState().withTitle("Das ist doch nur ein Test"));
  }

  @Override
  public Single<ItemCollectionResource> getCollection(Integer numItems, Boolean embedItems, Integer delayMs) {
    return Single.just(new ConsumerCollectionResourceImpl(context, numItems, embedItems, delayMs));
  }

  @Override
  public Single<ItemResource> getItemWithIndex(Integer index, Integer delayMs) {
    return Single.just(new ConsumerItemResourceImpl(context, index, delayMs));
  }

  @Override
  public Single<ExamplesEntryPointResource> getEntryPoint() {
    return Single.just(new ExamplesEntryPointResourceImpl(context));
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo(this).setTitle(
        "Examples for resources that fetch and modify linked or embedded resources with the HalApiClient");
  }

  @GET
  public void get(@Suspended AsyncResponse response) {
    context.respondWith(this, response);
  }

}
