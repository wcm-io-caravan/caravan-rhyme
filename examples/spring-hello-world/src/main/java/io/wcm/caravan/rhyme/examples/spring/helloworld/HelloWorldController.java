package io.wcm.caravan.rhyme.examples.spring.helloworld;

import static io.wcm.caravan.rhyme.examples.spring.helloworld.HelloWorldResource.TEXT_PARAM;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.resource.Link;

@RestController
class HelloWorldController {

  private static final String HELLO_WORLD = "Hello World!";

  private static final Map<String, String> TRANSLATIONS = ImmutableMap.of(
      "de", "Hallo Welt",
      "fr", "Bonjour le monde",
      "it", "Ciao mondo");

  @GetMapping("/")
  HelloWorldResource createDefaultResource() {

    return new AbstractHelloWorldResource() {

      @Override
      public String getText() {
        return HELLO_WORLD;
      }

      @Override
      public Link createLink() {

        WebMvcLinkBuilder linkBuilder = linkTo(methodOn(HelloWorldController.class).createDefaultResource());

        return new Link(linkBuilder.toString())
            .setTitle("The default Hello World message");
      }
    };
  }

  @GetMapping("/custom")
  HelloWorldResource createCustomResource(@RequestParam(name = TEXT_PARAM) String text) {

    return new AbstractHelloWorldResource() {

      @Override
      public String getText() {
        return text;
      }

      @Override
      public Link createLink() {

        WebMvcLinkBuilder linkBuilder = linkTo(methodOn(HelloWorldController.class).createCustomResource(text));

        return new Link(linkBuilder.toString())
            .setTitle(text == null ? "Load a resource with a customized message" : "A customized '" + text + "' message");
      }
    };
  }

  @GetMapping("/translations/{languageCode}")
  HelloWorldResource createTranslatedResource(@PathVariable String languageCode) {

    return new AbstractHelloWorldResource() {

      @Override
      public String getText() {

        return TRANSLATIONS.getOrDefault(languageCode, HELLO_WORLD);
      }

      @Override
      public Link createLink() {

        WebMvcLinkBuilder linkBuilder = linkTo(methodOn(HelloWorldController.class).createTranslatedResource(languageCode));

        String languageName = Locale.forLanguageTag(languageCode).getDisplayLanguage(Locale.ENGLISH);

        return new Link(linkBuilder.toString())
            .setTitle("A message in " + languageName)
            .setName(languageCode);
      }
    };
  }

  /**
   * Defines the methods that generate links to related resources, which are shared between the three available
   * variations of {@link HelloWorldResource} in this controller
   */
  private abstract class AbstractHelloWorldResource implements HelloWorldResource {

    private boolean isDefaultMessage() {
      return HELLO_WORLD.equals(getText());
    }

    @Override
    public Optional<HelloWorldResource> withDefaultMessage() {

      if (isDefaultMessage()) {
        return Optional.empty();
      }
      return Optional.of(createDefaultResource());
    }

    @Override
    public Stream<HelloWorldResource> getTranslations() {

      if (!isDefaultMessage()) {
        return Stream.empty();
      }
      return TRANSLATIONS.keySet().stream()
          .map(language -> createTranslatedResource(language));
    }

    @Override
    public HelloWorldResource withCustomMessage(String text) {

      return createCustomResource(text);
    }
  }
}
