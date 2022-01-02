package io.wcm.caravan.rhyme.examples.spring.helloworld;

import static io.wcm.caravan.rhyme.examples.spring.helloworld.HelloWorldResource.TEXT_PARAM;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@RestController
class HelloWorldController {

  private static final String HELLO_WORLD = "Hello World!";

  private static final Map<String, String> TRANSLATIONS = ImmutableMap.of(
      "de", "Hallo Welt",
      "fr", "Bonjour le monde",
      "it", "Ciao mondo");

  @GetMapping("/")
  HelloWorldResource getDefaultResource() {

    return new AbstractHelloWorldResource() {

      @Override
      public String getText() {
        return HELLO_WORLD;
      }

      @Override
      public Optional<HelloWorldResource> withDefaultMessage() {
        // no need to render a link to the default resource because this is the default resource
        return Optional.empty();
      }

      @Override
      public Link createLink() {

        return buildLinkTo(controller -> controller.getDefaultResource())
            .setTitle("The default 'Hello World' message");
      }
    };
  }

  @GetMapping("/custom")
  HelloWorldResource getCustomResource(@RequestParam(name = TEXT_PARAM) String text) {

    return new AbstractHelloWorldResource() {

      @Override
      public String getText() {
        return text;
      }

      @Override
      public Stream<HelloWorldResource> getTranslations() {
        // don't render links to translations from the resource with custom text
        return Stream.empty();
      }

      @Override
      public Link createLink() {

        return buildLinkTo(controller -> controller.getCustomResource(text))
            .setTitle(text == null ? "Load a resource with a customized message" : "A customized '" + text + "' message");
      }
    };
  }

  @GetMapping("/translations/{languageCode}")
  HelloWorldResource getTranslatedResource(@PathVariable String languageCode) {

    return new AbstractHelloWorldResource() {

      @Override
      public String getText() {

        return TRANSLATIONS.getOrDefault(languageCode, HELLO_WORLD);
      }

      @Override
      public Stream<HelloWorldResource> getTranslations() {

        return super.getTranslations()
            .filter(translation -> !translation.createLink().getName().equals(languageCode));
      }

      @Override
      public Link createLink() {

        String languageName = Locale.forLanguageTag(languageCode).getDisplayLanguage(Locale.ENGLISH);

        return buildLinkTo(controller -> controller.getTranslatedResource(languageCode))
            .setTitle("A message in " + languageName)
            .setName(languageCode);
      }
    };
  }

  /**
   * Builds a link pointing to a controller method with the help of the {@link WebMvcLinkBuilder} class
   * @param controllerCall a lambda that calls the target function on a subclass of the
   *          {@link HelloWorldController}
   * @return a wcm.io Caravan {@link Link} object where the href property is already set
   */
  private Link buildLinkTo(Function<HelloWorldController, LinkableResource> controllerCall) {

    WebMvcLinkBuilder linkBuilder = linkTo(controllerCall.apply(methodOn(HelloWorldController.class)));

    return new Link(linkBuilder.toString());
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

      return Optional.of(getDefaultResource());
    }

    @Override
    public Stream<HelloWorldResource> getTranslations() {

      return TRANSLATIONS.keySet().stream()
          .map(language -> getTranslatedResource(language));
    }

    @Override
    public HelloWorldResource withCustomMessage(String text) {

      return getCustomResource(text);
    }
  }
}
