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

import java.util.concurrent.TimeUnit;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.damnhandy.uri.template.UriTemplate;

import io.reactivex.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.collection.ItemState;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.hal.microservices.api.server.EmbeddableResource;
import io.wcm.caravan.hal.microservices.api.server.LinkableResource;
import io.wcm.caravan.hal.resource.Link;


/**
 * An example for a class that uses constructor injection of the context and parameters.
 * The advantage is that you only have a single constructor and final fields,
 * the disadvantage is that the JaxRsLinkBuilder must assume that the fields have the exact same name as the parameters
 */
@Path("/collections/items/{index}")
public class DelayableItemResourceImpl implements ItemResource, LinkableResource, EmbeddableResource {

  private final ExampleServiceRequestContext context;

  private final Integer index;
  private final Integer delayMs;

  private boolean embedded;

  public DelayableItemResourceImpl(@Context ExampleServiceRequestContext context,
      @PathParam("index") Integer index,
      @QueryParam("delayMs") Integer delayMs) {

    this.context = context;
    this.index = index;
    this.delayMs = delayMs;
  }

  @Override
  public Single<ItemState> getProperties() {

    ItemState item = new ItemState();
    item.title = getTitle();
    item.index = index;
    item.thread = Thread.currentThread().getName();

    Single<ItemState> properties = Single.just(item);
    if (delayMs != null && delayMs > 0) {
      properties = properties.delay(delayMs, TimeUnit.MILLISECONDS)
          .map(state -> {
            state.thread = Thread.currentThread().getName();
            return state;
          });
    }

    return properties;
  }

  @Override
  public Link createLink() {

    //return buildLinkManually();

    return context.buildLinkTo(this).setTitle(getTitle());
  }

  private Link buildLinkManually() {

    UriTemplate template = UriTemplate.buildFromTemplate(context.getContextPath() + "/collections/items/{index}{?delayMs,bundleVersion}")
        .build();

    if (index != null) {
      template.set("index", index);
      template.set("delayMs", delayMs);
    }

    template.set("bundleVersion", context.getBundleVersion());

    String uri = template.expandPartial();
    return new Link(uri).setTitle(getTitle());
  }

  private String getTitle() {
    String title;
    if (index != null) {
      title = "The item with index " + index;
      if (delayMs != null && delayMs.intValue() > 0) {
        title += " delayed by " + delayMs + "ms";
      }
    }
    else {
      title = "Load an item with a specific index, and simulate a delay during resource generation";
    }
    return title;
  }

  @Override
  public boolean isEmbedded() {
    return embedded;
  }

  DelayableItemResourceImpl setEmbedded(boolean value) {
    this.embedded = value;
    return this;
  }

  @GET
  public void get(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {
    context.respondWith(this, uriInfo, response);
  }

}
