/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.hal.api.common;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;

/**
 * An interface to be implemented by all resources that should be available via HTTP. If you don't implement this
 * interface in your server-side implementation of a HAL API interface, then that resource can only be embedded
 */
public interface LinkableResource {

  /**
   * Create a link to this resource, including meaningful title and name properties where appropriate. If all required
   * parameters of the resource are set, then the link will have a resolved URI as href property. If some or all
   * parameters are null, a link with a URI template is created instead
   * @return a link instance to be added to the context {@link HalResource}
   */
  Link createLink();
}
