package io.wcm.caravan.rhyme.awslambda.api;

import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * The strategy used by {@link RhymeRequestHandler} to create the server-side resource implementation to be rendered
 */
public interface LambdaResourceRouting {

  /**
   * Determines and instantiates the server-side resource implementation to be rendered for the
   * the incoming request (based on the path and/or query parameters)
   * @param rhyme instance for the incoming request
   * @return the server-side resource implementation to be rendered (or null if no resource was found for the given
   *         path)
   */
  LinkableResource createRequestedResource(LambdaRhyme rhyme);
}
