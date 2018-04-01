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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;

import io.wcm.caravan.hal.api.common.EmbeddableResource;
import io.wcm.caravan.hal.api.common.LinkableResource;
import io.wcm.caravan.hal.api.server.jaxrs.AsyncHalResponseHandler;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemState;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.resource.Link;
import rx.Observable;

@Path("/collections/items/{index}")
public class ItemResourceImpl implements ItemResource, LinkableResource, EmbeddableResource {

  @Context
  private ExampleServiceRequestContext context;

  @PathParam("index")
  private Integer index;

  private boolean embedded;

  public ItemResourceImpl() {

  }

  public ItemResourceImpl(ExampleServiceRequestContext context, Integer index) {
    this.context = context;
    this.index = index;
  }

  @Override
  public Observable<ItemState> getProperties() {

    ItemState item = new ItemState();
    item.index = index;
    item.uuid = null;

    return Observable.just(item);
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo(this)
        .setTitle("The item with index " + index);
  }

  @Override
  public boolean isEmbedded() {
    return embedded;
  }

  public ItemResourceImpl setEmbedded(boolean embedded) {
    this.embedded = embedded;
    return this;
  }

  @GET
  public void get(@Suspended AsyncResponse response) {
    AsyncHalResponseHandler.respond(this, response);
  }

}
