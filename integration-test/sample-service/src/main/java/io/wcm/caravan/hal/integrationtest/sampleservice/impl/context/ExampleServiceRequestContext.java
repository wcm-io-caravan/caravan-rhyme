/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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

import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import io.wcm.caravan.hal.api.common.LinkableResource;
import io.wcm.caravan.hal.api.server.jaxrs.JaxRsLinkBuilder;
import io.wcm.caravan.hal.resource.Link;

@Path("") // this annotation is important so that instances of this class can be injected into other resources using @Context
public class ExampleServiceRequestContext {

  private final String contextPath;

  public ExampleServiceRequestContext(@Context ExampleServiceOsgiComponent osgiContext) {
    this.contextPath = osgiContext.getContextPath();
  }

  public Link buildLinkTo(LinkableResource targetResource) {

    JaxRsLinkBuilder linkBuilder = new JaxRsLinkBuilder(contextPath, targetResource);

    return linkBuilder.build();
  }

  public String getContextPath() {
    return contextPath;
  }
}
