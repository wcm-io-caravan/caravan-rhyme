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
public class HelloWorldTest {

  private final HelloWorldResource helloWorld;

  HelloWorldTest(@Autowired ServletWebServerApplicationContext server) {

    HalApiClient client = HalApiClient.create();

    String entryPointUrl = "http://localhost:" + server.getWebServer().getPort();

    helloWorld = client.getRemoteResource(entryPointUrl, HelloWorldResource.class);
  }

  @Test
  void should_provide_default_message() {

    String defaultMsg = helloWorld.getText();

    assertThat(defaultMsg)
        .isEqualTo("Hello World!");
  }

  @Test
  void should_allow_to_fetch_custom_message() {

    String customMsg = helloWorld.withCustomMessage("Foo Bar!").getText();

    assertThat(customMsg)
        .isEqualTo("Foo Bar!");
  }

  @Test
  void should_allow_to_fetch_translated_messages() {

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
  void should_hide_link_to_default_message_on_entry_point() {

    Optional<HelloWorldResource> defaultLink = helloWorld.withDefaultMessage();

    assertThat(defaultLink)
        .isNotPresent();
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
}
