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
package io.wcm.caravan.reha.jaxrs.api;

import java.util.Map;
import java.util.function.Consumer;

import javax.ws.rs.core.Application;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.jaxrs.impl.JaxRsControllerProxyLinkBuilder;

/**
 * @param <JaxRsResourceType> the class of the {@link Component} annotated with {@link JaxrsResource}
 */
public interface JaxRsLinkBuilder<JaxRsResourceType> {

  /**
   * @param parameters to append to the parameters
   * @return this
   */
  JaxRsLinkBuilder<JaxRsResourceType> withAdditionalQueryParameters(Map<String, Object> parameters);

  /**
   * @param consumer a function to be called on the proxy that fills in the parameters for the resource
   * @return a {@link Link} instance with the href already set
   */
  Link buildLinkTo(Consumer<JaxRsResourceType> consumer);

  /**
   * Factory method to create {@link JaxRsLinkBuilder} instances
   * @param baseUrl the base path of the JAX-RS {@link Application}
   * @param resourceClass the class of the {@link Component} annotated with {@link JaxrsResource}
   * @return the created instance
   */
  static <JaxRsResourceType> JaxRsLinkBuilder<JaxRsResourceType> create(String baseUrl, Class<JaxRsResourceType> resourceClass) {

    return new JaxRsControllerProxyLinkBuilder<JaxRsResourceType>(baseUrl, resourceClass);
  }

}
