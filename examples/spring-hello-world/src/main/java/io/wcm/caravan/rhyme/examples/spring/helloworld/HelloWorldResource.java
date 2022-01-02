package io.wcm.caravan.rhyme.examples.spring.helloworld;

import java.util.Optional;
import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.annotations.Related;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.annotations.TemplateVariable;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * An example for a simple HAL API that provides a greeting message, and has additional links to translate or customize
 * this message
 */
@HalApiInterface
public interface HelloWorldResource extends LinkableResource {

  static final String TEXT_PARAM = "text";
  static final String LANGUAGE_PARAM = "language";

  /**
   * @return the text of the greeting message
   */
  @ResourceProperty
  String getText();

  /**
   * A link template to load a customized greeting message
   * @param text the custom text for the greeting message
   * @return a {@link HelloWorldResource} for which {@link #getText()} will return the given text
   */
  @Related("hello:custom")
  HelloWorldResource withCustomMessage(@TemplateVariable(TEXT_PARAM) String text);

  /**
   * Links to all available translated versions of the resource (using the language code as link name)
   * @return 1..n {@link HelloWorldResource} instances
   */
  @Related("hello:translated")
  Stream<HelloWorldResource> getTranslations();

  /**
   * @return A link back to the default 'Hello World!' message resource (which is only present on translated and
   *         customized resources)
   */
  @Related("hello:default")
  Optional<HelloWorldResource> withDefaultMessage();
}
