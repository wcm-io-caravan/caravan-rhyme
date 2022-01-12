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

import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * An intermediate stage during link generation that is returned by
 * {@link UrlFingerprinting#createLinkWith(WebMvcLinkBuilder)} and allows
 * adding adding name and title attributes before calling {@link #build()} to finally create the link.
 * @see UrlFingerprinting
 */
public interface RhymeLinkBuilder {

  /**
   * @param name the name attribute for the generated link
   * @return this
   */
  RhymeLinkBuilder withName(String name);

  /**
   * @param title the title attribute for the generated link
   * @return this
   */
  RhymeLinkBuilder withTitle(String title);

  /**
   * Used to set a different title attribute only if the generated link contains URI template variables
   * @param title the title attribute for the generated link template
   * @return this
   */
  RhymeLinkBuilder withTemplateTitle(String title);

  /**
   * Ensures that a link template is generated that contains query parameter variables with all the given names.
   * If the link already contains resolved query parameters with the same names, they are removed.
   * @param names of the query parameter variables to add
   * @return this
   */
  RhymeLinkBuilder withTemplateVariables(String... names);

  /**
   * Allows to disable the addition of fingerprinting parameters only for the link that is currently being built
   * @return this
   */
  RhymeLinkBuilder withoutFingerprint();

  /**
   * Finishes building the link
   * @return a fully populated {@link Link} instance to be returned by your implementation of
   *         {@link LinkableResource#createLink()}
   */
  Link build();

  /**
   * Finishes building the link and create a proxy instance that returns this link when
   * {@link LinkableResource#createLink()} is called.
   * <p>
   * This can be used to implement a method annotated with
   * {@link Related} where you can't create a full implementation of a given {@link HalApiInterface},
   * but you can create a {@link Link} with a URI pointing to such a resource.
   * </p>
   * @param <T> the return type
   * @param halApiInterface an interface annotated with {@link HalApiInterface}
   * @return a proxy instance on which only {@link LinkableResource#createLink()} can be called
   */
  <T extends LinkableResource> T buildLinked(Class<T> halApiInterface);
}
