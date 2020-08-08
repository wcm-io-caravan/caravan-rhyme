/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
package io.wcm.caravan.reha.jaxrs.impl;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import io.wcm.caravan.reha.api.server.LinkBuilder;

/**
 * Creates JAX-RS specific instance of {@link LinkBuilder} that are able to automatically build links to server-side
 * resource instances by scanning their classes for {@link Path}, {@link QueryParam}, {@link BeanParam} and
 * {@link PathParam} annotations
 */
public final class JaxRsLinkBuilderFactory {

  // it's important to reuse this instance, because it contains shared caches to reduce the performance overhead of annotation lookup for each class
  private static final JaxRsLinkBuilderSupport SUPPORT = new JaxRsLinkBuilderSupport();

  private JaxRsLinkBuilderFactory() {
    // static methods only
  }

  /**
   * @param baseUrl for all resources of this service
   * @return a {@link LinkBuilder} instance
   */
  public static LinkBuilder createLinkBuilder(String baseUrl) {
    return LinkBuilder.create(baseUrl, SUPPORT);
  }
}
