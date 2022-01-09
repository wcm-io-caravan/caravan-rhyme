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
package io.wcm.caravan.rhyme.api.server;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils;

/**
 * Contains utility methods to perform conversions between resources that are semantically identical, but
 * need some tweeks for the framework to properly handle them
 */
public final class ResourceConversions {

  private ResourceConversions() {
    // only static methods in this class
  }

  /**
   * Create a proxy of a resource that also implements {@link EmbeddableResource}. This can be useful if you have
   * fetched some resources from an upstream server, and want to embed them in your own output (and include links
   * to the original resources).
   * @param <T> the interface with the {@link HalApiInterface} annotation
   * @param linkableResource a resource fetched with the {@link HalApiClient}, or a server-side resource implementation
   *          that does not implement {@link EmbeddableResource}
   * @return a proxy instance that implements {@link EmbeddableResource} and all interfaces the given object implements
   */
  public static <T> T asEmbeddedResource(T linkableResource) {

    return HalApiReflectionUtils.createEmbeddedResourceProxy(linkableResource, true);
  }

  /**
   * Create a proxy of a resource that also implements {@link EmbeddableResource}, with
   * {@link EmbeddableResource#isLinkedWhenEmbedded()} returning false. This can be useful if you have
   * fetched some resources from an upstream server, and want to embed them in your own output (but not include any
   * links to those embedded resources).
   * @param <T> the interface with the {@link HalApiInterface} annotation
   * @param linkableResource a resource fetched with the {@link HalApiClient}, or a server-side resource implementation
   *          that does not implement {@link EmbeddableResource}
   * @return a proxy instance that implements {@link EmbeddableResource} and all interfaces the given object implements
   */
  public static <T> T asEmbeddedResourceWithoutLink(T linkableResource) {

    return HalApiReflectionUtils.createEmbeddedResourceProxy(linkableResource, false);
  }

  /**
   * Create a proxy for the given interface that can only be used to create links to that resource.
   * This can be returned from server-side implementations of {@link Related} methods if it's not feasible to
   * create a complete resource implementation, but you do have a {@link Link} pointing to a URI where a
   * resource of the given interface can be retrieved.
   * <p>
   * Note that calling any other method than {@link LinkableResource#createLink()} on the proxy will fail.
   * </p>
   * @param link to be returned by {@link LinkableResource#createLink()}
   * @param halApiInterface the interface that should be implemented
   * @return a proxy instance implementing {@link LinkableResource} which you can only call
   *         {@link LinkableResource#createLink()}
   */
  public static <T> T asLinkableResource(Link link, Class<T> halApiInterface) {

    return HalApiReflectionUtils.createLinkableResourceProxy(link, halApiInterface);
  }

}
