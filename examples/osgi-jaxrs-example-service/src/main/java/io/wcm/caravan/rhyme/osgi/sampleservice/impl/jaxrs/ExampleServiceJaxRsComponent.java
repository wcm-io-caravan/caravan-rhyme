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
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.jaxrs;

import java.util.function.Function;

import javax.ws.rs.BeanParam;
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
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.caravan.api.CaravanRhyme;
import io.wcm.caravan.rhyme.caravan.api.CaravanRhymeRequestCycle;
import io.wcm.caravan.rhyme.jaxrs.api.JaxRsBundleInfo;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.caching.CachingExamplesResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.caching.EvenAndOddItemsResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.ClientCollectionResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.ClientItemResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.CollectionExamplesResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.CollectionParametersBean;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.DelayableCollectionResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.DelayableItemResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors.ErrorParametersBean;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors.ErrorsExamplesResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors.HalApiClientErrorResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors.ServerSideErrorResourceImpl;

@Component(service = ExampleServiceJaxRsComponent.class, scope = ServiceScope.PROTOTYPE)
@JaxrsResource
@JaxrsApplicationSelect(ExampleServiceJaxRsApplication.SELECTOR)
public class ExampleServiceJaxRsComponent {

  @Reference
  private CaravanRhymeRequestCycle requestCycle;

  @Reference
  private JaxRsBundleInfo bundleInfo;

  private ExampleServiceRequestContext createRequestContext(CaravanRhyme rhyme) {
    return new ExampleServiceRequestContext(rhyme, bundleInfo);
  }

  private void renderResource(UriInfo uriInfo, AsyncResponse response, Function<ExampleServiceRequestContext, LinkableResource> resourceImplConstructor) {

    requestCycle.processRequest(uriInfo, response, this::createRequestContext, resourceImplConstructor);
  }

  @GET
  @Path("/")
  public void getEntryPoint(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {

    renderResource(uriInfo, response,
        request -> new ExamplesEntryPointResourceImpl(request, request.hasFingerPrintedUrl()));
  }

  @GET
  @Path("/caching")
  public void getCachingExamples(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {

    renderResource(uriInfo, response,
        request -> new CachingExamplesResourceImpl(request));
  }

  @GET
  @Path("/caching/evenAndOdd")
  public void getEvenAndOdd(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @BeanParam CollectionParametersBean parameters) {

    renderResource(uriInfo, response,
        request -> new EvenAndOddItemsResourceImpl(request, parameters));
  }

  @GET
  @Path("/collections")
  public void getCollectionExamples(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {

    renderResource(uriInfo, response,
        request -> new CollectionExamplesResourceImpl(request));
  }

  @GET
  @Path("/collections/items")
  public void getDelayableCollection(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @BeanParam CollectionParametersBean parameters) {

    renderResource(uriInfo, response,
        request -> new DelayableCollectionResourceImpl(request, parameters));
  }

  @GET
  @Path("/collections/items/{index}")
  public void getDelayableItem(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @PathParam("index") Integer index,
      @QueryParam("delayMs") Integer delayMs) {

    renderResource(uriInfo, response,
        request -> new DelayableItemResourceImpl(request, index, delayMs));
  }

  @GET
  @Path("/collection/proxy/items")
  public void getClientCollection(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @BeanParam CollectionParametersBean parameters) {

    renderResource(uriInfo, response,
        request -> new ClientCollectionResourceImpl(request, parameters));
  }

  @GET
  @Path("/collections/proxy/items/{index}")
  public void getClientItem(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @PathParam("index") Integer index,
      @QueryParam("delayMs") Integer delayMs) {

    renderResource(uriInfo, response,
        request -> new ClientItemResourceImpl(request, index, delayMs));
  }

  @GET
  @Path("/errors")
  public void getErrorsExamples(@Context UriInfo uriInfo, @Suspended AsyncResponse response) {

    renderResource(uriInfo, response,
        request -> new ErrorsExamplesResourceImpl(request));
  }

  @GET
  @Path("/errors/client")
  public void getHalApiClientError(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @BeanParam ErrorParametersBean parameters) {

    renderResource(uriInfo, response,
        request -> new HalApiClientErrorResourceImpl(request, parameters));
  }

  @GET
  @Path("/errors/server")
  public void getServerSideError(@Context UriInfo uriInfo, @Suspended AsyncResponse response,
      @BeanParam ErrorParametersBean parameters) {

    renderResource(uriInfo, response,
        request -> new ServerSideErrorResourceImpl(request, parameters));
  }
}
