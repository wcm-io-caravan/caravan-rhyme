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
package io.wcm.caravan.rhyme.spring.api;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.context.annotation.RequestScope;


/**
 * Adds support for URL fingerprinting to your server-side resource implementations, by ensuring
 * that all links in your service are using URLs that are guaranteed to change whenever the
 * data, code or configuration of your service is updated. Responses for any URL that contains such a fingerprint
 * can be considered immutable and cached indefinitely by the clients. If data (or code) changes, the clients will still
 * pick up the most version of the data because the API entry point will point to different URLs with a new fingerprint.
 * <p>
 * To enable URL fingerprinting in your service, you need to do the following:
 * </p>
 * <ol>
 * <li>Define a central location in your code where you call {@link SpringRhyme#enableUrlFingerprinting()} to create
 * and configure this instance. In a Spring application, a {@link RequestScope} component works well as long as you
 * stick to
 * single-threaded request processing</li>
 * <li>Call {@link #withTimestampParameter(String, Supplier)} to define the name of a
 * query parameter to be used for URL fingerprinting, and the source for a last modification date</li>
 * <li>Call {@link #withConditionalMaxAge(Duration, Duration)} to enable modification
 * of your response's max-age cache-control directive depending on the presence of the URL fingerprint</li>
 * <li>Ensure that all the links between your server-side resources are then created using
 * {@link #createLinkWith(WebMvcLinkBuilder)} of the same configured {@link UrlFingerprinting} instance</li>
 * </ol>
 */
public interface UrlFingerprinting {

  /**
   * Add an URL fingerprinting parameter based on a last modification date
   * @param name of the query parameter to be added
   * @param source a function to retrieve the value of this parameter if it is not already present in the incoming
   *          request
   * @return this instance
   */
  UrlFingerprinting withTimestampParameter(String name, Supplier<Instant> source);

  /**
   * Adds another additional query parameter to all links being built with this instance
   * (unless a query parameter with the same name already exists)
   * @param name of the query parameter to be added
   * @param value of the parameter to be added
   * @return this
   */
  UrlFingerprinting withQueryParameter(String name, Object value);

  /**
   * Allows the URL fingerprinting logic to modify the max-age cache-control directive for the response that is
   * currently rendered, depending on whether the incoming request is using a fingerprinted URL.
   * @param mutableMaxAge to use for the entry point (or any other resources without a fingerprint in the URL).
   * @param immutableMaxAge to use for all resources that do contain a fingerprint in their URL.
   * @return this instance
   */
  UrlFingerprinting withConditionalMaxAge(Duration mutableMaxAge, Duration immutableMaxAge);

  /**
   * Determines if the current incoming request is using URL fingerprinting.
   * @return true if the query contains values for all timestamp parameters defined with
   *         {@link #withTimestampParameter(String, Supplier)}
   */
  boolean isUsedInIncomingRequest();

  /**
   * Creates a {@link RhymeLinkBuilder} that generates the URL to a controller method and adds the
   * additional URL fingerprinting parameters
   * @param linkBuilder created with {@link WebMvcLinkBuilder#linkTo(Class)} and
   *          {@link WebMvcLinkBuilder#methodOn(Class, Object...)}
   * @return a {@link RhymeLinkBuilder} to decorate the link with name and title attributes, and
   *         then finally build it
   */
  RhymeLinkBuilder createLinkWith(WebMvcLinkBuilder linkBuilder);

}
