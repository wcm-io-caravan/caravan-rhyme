/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.api.common;

import io.wcm.caravan.hal.api.annotations.RelatedResource;

/**
 * An interface that server-side resources should implement if it's reasonable to embed their HAL representation into
 * their context resource.
 * For each related resource that implements this interface, {@link #isEmbedded()} will be called (when the resource
 * is rendered server-side) to determine whether to embed the resource. It's up to the resource implementation
 * to either decide this on it's own (e.g. depending on the amount of content), or also provide a setter so that other
 * resources can decide this in the implementation of the method that is annotated with {@link RelatedResource}
 */
public interface EmbeddableResource {

  /**
   * Determines if this resource should be embedded rather then linked to from the context resource that created the
   * resource implementation instance
   * @return true if this resource should be embedded
   */
  boolean isEmbedded();
}
