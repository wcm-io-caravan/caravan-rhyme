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

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;

/**
 * An interface that server-side resources should implement if it's reasonable to embed them into
 * the context resource.
 * <p>
 * For each related resource that implements this interface, {@link #isEmbedded()} will be called (when the resource
 * is rendered) to determine whether to embed the resource. It's up to the resource implementation
 * to either decide this on its own (e.g. depending on the amount of content), or also provide a setter so that other
 * resources can decide this after creating a resource instance to which they are linking to.
 * </p>
 * <p>
 * You shouldn't make your <b>interfaces</b> annotated with {@link HalApiInterface}
 * extend this interface (since it's only being used server-side, and clients shouldn't need to know whether a resource
 * is embedded or linked).
 * </p>
 * @see LinkableResource
 * @see HalApiInterface
 */
@ConsumerType
public interface EmbeddableResource {

  /**
   * Determines if this resource should be embedded (rather than just linked to) from the context resource that created
   * the resource implementation instance
   * @return true if this resource should be embedded
   */
  default boolean isEmbedded() {
    return true;
  }

  /**
   * Determines if this resource should also be linked when it's already embedded in its context resource
   * @return true if links to the embedded resources should be included
   */
  default boolean isLinkedWhenEmbedded() {
    return true;
  }
}
