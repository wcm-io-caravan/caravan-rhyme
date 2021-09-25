package io.wcm.caravan.rhyme.spring.api;

import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * An intermediate stage during link generation that is returned by
 * {@link UrlFingerprinting#createLinkWith(WebMvcLinkBuilder)} that allows
 * adding adding name and title attributes before calling {@link #build()} to finally create the link.
 * @see UrlFingerprinting
 */
public interface RhymeLinkBuilder {

  /**
   * Allows to conditionally disable the URL fingerprinting for the link being build
   * @param condition true if URL fingerprinting parameters should be added
   * @return this
   */
  RhymeLinkBuilder withFingerprintingOnlyIf(boolean condition);

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
   * Finishes building the link
   * @return a fully populated {@link Link} instance to be returned by your implementation of
   *         {@link LinkableResource#createLink()}
   */
  Link build();

}
