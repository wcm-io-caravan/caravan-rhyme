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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection;

import static io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl.SERVICE_PATH;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.CollectionExamplesResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.CollectionParameters;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.TitledState;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;

@Component(service = CollectionExamplesResourceImpl.class, scope = ServiceScope.PROTOTYPE)
@JaxrsResource
@Path(SERVICE_PATH + "/collections")
public class CollectionExamplesResourceImpl implements CollectionExamplesResource, LinkableResource {

  @Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
  private ExampleServiceRequestContext context;

  public CollectionExamplesResourceImpl() {

  }

  public CollectionExamplesResourceImpl(ExampleServiceRequestContext context) {
    this.context = context;
  }

  @Override
  public Single<TitledState> getState() {
    return Single.just(new TitledState());
  }

  @Override
  public Single<ItemCollectionResource> getCollection(CollectionParameters params) {
    return Single.just(new DelayableCollectionResourceImpl(context, null));
  }

  @Override
  public Single<ItemResource> getItem(Integer index, Integer delayMs) {
    return Single.just(new DelayableItemResourceImpl(context, index, delayMs));
  }

  @Override
  public Single<ItemCollectionResource> getCollectionThroughClient(CollectionParameters params) {
    return Single.just(new ClientCollectionResourceImpl(context, null));
  }

  @Override
  public Single<ItemResource> getItemThroughClient(Integer index, Integer delayMs) {
    return Single.just(new ClientItemResourceImpl(context, index, delayMs));
  }

  @Override
  public Single<ExamplesEntryPointResource> getEntryPoint() {
    return Single.just(new ExamplesEntryPointResourceImpl(context));
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo(this)
        .setTitle("Examples for handling collections of linked or embedded resources");
  }

  @GET
  public void get(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {
    context.respondWith(uriInfo, this, response);
  }


}
