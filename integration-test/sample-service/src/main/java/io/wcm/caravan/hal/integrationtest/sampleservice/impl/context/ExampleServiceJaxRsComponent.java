/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2020 wcm.io
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
package io.wcm.caravan.hal.integrationtest.sampleservice.impl.context;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.caching.CachingExamplesResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.caching.EvenAndOddItemsResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.ClientCollectionResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.ClientItemResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.CollectionExamplesResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.CollectionParametersImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.DelayableCollectionResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.collection.DelayableItemResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.errors.ErrorsExamplesResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.errors.HalApiClientErrorResourceImpl;
import io.wcm.caravan.hal.integrationtest.sampleservice.impl.resource.errors.ServerSideErrorResourceImpl;

@Component(service = ExampleServiceJaxRsComponent.class, scope = ServiceScope.PROTOTYPE)
@JaxrsResource
@JaxrsApplicationSelect(ExampleServiceApplication.SELECTOR)
public class ExampleServiceJaxRsComponent {

  @Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
  private ExampleServiceRequestContext context;

  @GET
  @Path("")
  public void getEntryPoint(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {

    context.limitMaxAge(60);

    context.respondWith(uriInfo, new ExamplesEntryPointResourceImpl(context), response);
  }

  @GET
  @Path("/caching")
  public void getCachingExamples(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {

    context.respondWith(uriInfo, new CachingExamplesResourceImpl(context), response);
  }

  @GET
  @Path("/caching/evenAndOdd")
  public void getEvenAndOdd(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @BeanParam CollectionParametersImpl parameters) {

    context.respondWith(uriInfo, new EvenAndOddItemsResourceImpl(context, parameters), response);
  }

  @GET
  @Path("/collections")
  public void getCollectionExamples(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {

    context.respondWith(uriInfo, new CollectionExamplesResourceImpl(context), response);
  }

  @GET
  @Path("/collections/items")
  public void getDelayableCollection(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @BeanParam CollectionParametersImpl parameters) {

    context.respondWith(uriInfo, new DelayableCollectionResourceImpl(context, parameters), response);
  }

  @GET
  @Path("/collections/items/{index}")
  public void getDelayableItem(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @PathParam("index") Integer index,
      @QueryParam("delayMs") Integer delayMs) {

    context.respondWith(uriInfo, new DelayableItemResourceImpl(context, index, delayMs), response);
  }

  @GET
  @Path("/collection/client/items")
  public void getClientCollection(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @BeanParam CollectionParametersImpl parameters) {

    context.respondWith(uriInfo, new ClientCollectionResourceImpl(context, parameters), response);
  }

  @GET
  @Path("/collections/client/items/{index}")
  public void getClientItem(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @PathParam("index") Integer index,
      @QueryParam("delayMs") Integer delayMs) {

    context.respondWith(uriInfo, new ClientItemResourceImpl(context, index, delayMs), response);
  }


  @GET
  @Path("/errors")
  public void getErrorsExamples(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {

    context.respondWith(uriInfo, new ErrorsExamplesResourceImpl(context), response);
  }

  @GET
  @Path("/errors/halApiClient")
  public void getHalApiClientError(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @QueryParam("statusCode") @DefaultValue("404") Integer statusCode,
      @QueryParam("message") @DefaultValue("default error message") String message,
      @QueryParam("withCause") @DefaultValue("false") Boolean withCause) {

    context.respondWith(uriInfo, new HalApiClientErrorResourceImpl(context, statusCode, message, withCause), response);
  }

  @GET
  @Path("/errors/serverSide")
  public void getServerSideError(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @QueryParam("statusCode") @DefaultValue("404") Integer statusCode,
      @QueryParam("message") @DefaultValue("default error message") String message,
      @QueryParam("withCause") @DefaultValue("false") Boolean withCause) {

    context.respondWith(uriInfo, new ServerSideErrorResourceImpl(context, statusCode, message, withCause), response);
  }
}
