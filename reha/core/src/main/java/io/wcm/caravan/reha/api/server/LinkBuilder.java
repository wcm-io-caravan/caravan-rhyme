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
package io.wcm.caravan.reha.api.server;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.impl.links.LinkBuilderImpl;

/**
 * A builder for {@link Link} instances that can be used when implementing {@link LinkableResource#createLink()}
 * in your server-side resource implementations.
 */
@ProviderType
public interface LinkBuilder {

  /**
   * @param parameters additional names and values of query parameters that should be added to the link
   * @return this
   */
  LinkBuilder withAdditionalParameters(Map<String, Object> parameters);

  // TODO: this interface as it is is only useful if implementations use annotations or reflections
  // to read the available template variables and values (e.g. the JaxRsLinkBuilder).
  // There should be another implementation where the link is build programmatically

  /**
   * @param resource the resource to which the link should be pointing
   * @return a link to the given resource
   */
  Link buildLinkTo(LinkableResource resource);

  /**
   * @param baseUrl the absolute base URL for all resources for which the link builder will be used
   * @param support implements the logic of extracting resource path and template variables from a server-side
   *          resource instance
   * @return a {@link LinkBuilder} instance
   */
  static LinkBuilder create(String baseUrl, LinkBuilderSupport support) {
    return new LinkBuilderImpl(baseUrl, support);
  }

}
