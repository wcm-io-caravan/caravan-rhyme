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
package io.wcm.caravan.rhyme.api.resources;

import org.osgi.annotation.versioning.ConsumerType;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceLink;

/**
 * An interface to be implemented by all resources that should be accessible directly via a URL (and therefore can be
 * linked to from another resource).
 * <p>
 * You <b>can</b> make your {@link HalApiInterface}s extends this interface directly, and doing so will make it possible
 * for clients to extract the resource link and URL from a client proxy by calling {@link #createLink()}. This can be
 * useful if the client need to select one of multiple links based on the link properties, or do other processing that
 * requires the knowledge of the URLs.
 * </p>
 * <p>
 * You may want want to keep the option of having certain small resources only be available as embedded resource, and
 * not let your clients know that they can also fetch an it directly through an URL. In that case, you would <b>not</b>
 * make your <b>interface</b> extend {@link LinkableResource}, but only implement it in the server-side implementation
 * class.
 * </p>
 * <p>
 * If you don't implement this interface in your server-side implementation of a HAL API interface, then that resource
 * cannot be rendered on its own, but only be embedded (and you must implement {@link EmbeddableResource} instead).
 * </p>
 * @see EmbeddableResource
 * @see HalApiInterface
 */
@ConsumerType
public interface LinkableResource {

  /**
   * Create a link to this resource, including meaningful title and name properties where appropriate. If all required
   * parameters of the resource are set, then the link should have a resolved URI as href property. If some or all
   * required parameters are null, a link with a URI template should be created instead.
   * <p>
   * Note that the Link instance doesn't have a relation property, because that is derived from the {@link Related}
   * annotation of the method that defines the links to this resource. The same resource can be linked from multiple
   * other resources using different relations, but all links are created on the server side using the same method.
   * </p>
   * @return a {@link Link} instance where href, title and name properties are already set as required
   */
  @ResourceLink
  Link createLink();
}
