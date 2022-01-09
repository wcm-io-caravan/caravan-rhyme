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

  RhymeLinkBuilder withTemplateVariables(String... queryParameterNames);

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

  <T extends LinkableResource> T buildProxyOf(Class<T> halApiInterface);
}
