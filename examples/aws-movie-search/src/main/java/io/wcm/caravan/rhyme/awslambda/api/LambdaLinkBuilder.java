package io.wcm.caravan.rhyme.awslambda.api;

import io.wcm.caravan.hal.resource.Link;

/**
 * A fluent builder for {@link Link} instances pointing to an AWS Lambda based resource implementation.
 * Can be created through {@link LambdaRhyme#buildLinkTo(String)} and will take the configured API gateway and AWS
 * staging prefix into account.
 */
public interface LambdaLinkBuilder {

  /**
   * @param name the name of the query parameter
   * @param value the value (can be null if a link template should be generated)
   * @return this
   */
  LambdaLinkBuilder addQueryVariable(String name, Object value);

  /**
   * Builds the link (that can be templated if any query variable was added with a null value=
   * @return a {@link Link} instance where the href attribute is already set
   */
  Link build();
}
