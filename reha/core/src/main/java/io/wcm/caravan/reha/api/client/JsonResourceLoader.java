/* Copyright (c) pro!vision GmbH. All rights reserved. */
package io.wcm.caravan.reha.api.client;

import org.osgi.annotation.versioning.ConsumerType;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.reha.api.common.HalResponse;

/**
 * An interface to delegate the actual loading and caching of a JSON+HAL resource via HTTP to any other HTTP client
 * library
 */
@FunctionalInterface
@ConsumerType
public interface JsonResourceLoader {

  /**
   * @param uri the URI of the resource to load. The exact format of the URI (i.e. whether it is fully qualified or not)
   *          depends on how the links are represented in the upstream resources
   * @return a {@link Single} that emits a {@link HalResponse} entity if the request was successful, or otherwise fails
   *         with an{@link HalApiClientException}
   */
  Single<HalResponse> loadJsonResource(String uri);
}
