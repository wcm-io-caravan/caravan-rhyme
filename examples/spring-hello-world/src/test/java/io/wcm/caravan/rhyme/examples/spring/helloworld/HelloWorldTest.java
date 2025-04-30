package io.wcm.caravan.rhyme.examples.spring.helloworld;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.client.HalApiClient;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class HelloWorldTest {

  private final HelloWorldResource helloWorld;

  HelloWorldTest(@Autowired ServletWebServerApplicationContext server) {

    HalApiClient client = HalApiClient.create();

    String entryPointUrl = "http://localhost:" + server.getWebServer().getPort();

    helloWorld = client.getRemoteResource(entryPointUrl, HelloWorldResource.class);
  }

  @Test
  void default_message_can_be_fetched() {

    String defaultMsg = helloWorld.getText();

    assertThat(defaultMsg)
        .isEqualTo("Hello World!");
  }

  @Test
  void custom_message_can_be_fetched() {

    String customMsg = helloWorld.withCustomMessage("Foo Bar!").getText();

    assertThat(customMsg)
        .isEqualTo("Foo Bar!");
  }

  @Test
  void translated_messages_can_be_fetched() {

    Stream<String> translations = helloWorld.getTranslations().map(HelloWorldResource::getText);

    assertThat(translations)
        .containsExactly("Hallo Welt", "Bonjour le monde", "Ciao mondo");
  }

  @Test
  void links_to_translated_messages_should_have_language_as_link_names() {

    Stream<Link> translationLinks = helloWorld.getTranslations().map(HelloWorldResource::createLink);

    assertThat(translationLinks)
        .extracting(Link::getName)
        .containsExactly("de", "fr", "it");
  }

  @Test
  void translated_message_should_have_only_links_to_other_translations() {

    HelloWorldResource firstTranslation = helloWorld.getTranslations().findFirst().get();

    String firstLanguage = firstTranslation.createLink().getName();

    assertThat(firstTranslation.getTranslations())
        .hasSize(2)
        .extracting(resource -> resource.createLink().getName())
        .doesNotContain(firstLanguage);
  }

  @Test
  void custom_message_should_not_have_links_to_translations() {

    Stream<HelloWorldResource> translations = helloWorld.withCustomMessage("Foo Bar!").getTranslations();

    assertThat(translations)
        .isEmpty();
  }

  @Test
  void custom_message_resource_should_have_link_back_to_default_message() {

    Optional<HelloWorldResource> defaultLink = helloWorld.withCustomMessage("Foo Bar!").withDefaultMessage();

    assertThat(defaultLink)
        .isPresent()
        .get()
        .extracting(HelloWorldResource::getText)
        .isEqualTo("Hello World!");
  }

  @Test
  void translated_message_resource_should_have_link_back_to_default_message() {

    HelloWorldResource firstTranslation = helloWorld.getTranslations().findFirst().get();

    Optional<HelloWorldResource> defaultLink = firstTranslation.withDefaultMessage();

    assertThat(defaultLink)
        .isPresent()
        .get()
        .extracting(HelloWorldResource::getText)
        .isEqualTo("Hello World!");
  }

  @Test
  void default_message_should_not_have_default_link() {

    Optional<HelloWorldResource> defaultLink = helloWorld.withDefaultMessage();

    assertThat(defaultLink)
        .isNotPresent();
  }
}
