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
package io.wcm.caravan.rhyme.jaxrs.impl.docs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;


/**
 * A Jax-RS resource that serves the HTML documentation generated with the rhyme-docs-maven-plugin
 */
@Component(service = RhymeDocsHtmlResource.class)
@JaxrsResource
@JaxrsApplicationSelect(RhymeDocsJaxRsApplication.SELECTOR)
public class RhymeDocsHtmlResource {

  @Reference
  private RhymeDocsOsgiBundleSupport rhymeDocs;

  /**
   * @param fileName of a documentation file in the bundle resources
   * @return the content of the file
   */
  @GET
  @Path("{fileName}")
  @Produces("text/html; charset=UTF-8")
  public String getHtmlDocumentation(@PathParam("fileName") String fileName) {

    try {
      return RhymeDocsSupport.loadGeneratedHtml(rhymeDocs, fileName);
    }
    catch (HalApiServerException ex) {
      throw new WebApplicationException(ex, ex.getStatusCode());
    }
  }
}
