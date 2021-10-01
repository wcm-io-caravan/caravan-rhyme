package io.wcm.caravan.rhyme.spring.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

/**
 * An extension that starts a {@link SpringBootApplication} using it's main method,
 * waits for startup to be complete and then injects a Rhyme client proxy
 * to fetch the entry point into any methods or a public field of the integration test class. For this to work, you
 * must annotated your test class, e.g.
 *
 * <pre>
 * &#64;ExtendWith(SpringRhymeIntegrationTestExtension.class)
 * &#64;SpringRhymeIntegrationTest(entryPointUri = "http://localhost:8080", applicationClass=YourSpringBootApplication.class)
 * </pre>
 *
 * The extension will then identify the parameter (or public field) of a type annotated with {@link HalApiInterface},
 * and create a client proxy implementation for that, using the entry point from the annotation.
 * @see HalApiClient
 * @see SpringRhymeIntegrationTest
 */
public class SpringRhymeIntegrationTestExtension implements BeforeAllCallback, TestInstancePostProcessor, ParameterResolver {

  private static ApplicationMainThread thread;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {

    // TODO: use a Store to keep this thread alife while the tests are executed
    if (thread == null) {

      thread = new ApplicationMainThread(getApplicationClass(context), new String[0]);
      thread.start();

      thread.waitUntilEntryPointCanBeLoaded(getEntryPointUri(context));
    }
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {

    getEntryPointResourceField(context)
        .ifPresent(field -> injectProxyIntoField(getEntryPointUri(context), testInstance, field));
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {

    return parameterContext.getParameter().getType().isAnnotationPresent(HalApiInterface.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {

    return createClientProxy(getEntryPointUri(extensionContext), parameterContext.getParameter().getType());
  }

  public void injectProxyIntoField(String entryPointUri, Object testInstance, Field field) {

    Object clientProxy = createClientProxy(entryPointUri, field.getType());

    try {
      field.set(testInstance, clientProxy);
    }
    catch (Throwable ex) {
      throw new HalApiDeveloperException("Failed to set field " + field.getName() + " for test " + field.getDeclaringClass() + ", make sure it's public", ex);
    }
  }

  public Object createClientProxy(String entryPointUri, Class<?> halApiInterface) {

    HalApiClient client = HalApiClient.create();

    return client.getRemoteResource(entryPointUri, halApiInterface);
  }

  private static Optional<Field> getEntryPointResourceField(ExtensionContext context) {

    return context.getTestClass().map(testClass -> {

      List<Field> resourceFields = FieldUtils.getAllFieldsList(testClass).stream()
          .filter(field -> field.getDeclaringClass().equals(testClass))
          .filter(field -> field.isAccessible())
          .filter(field -> field.getType().isAnnotationPresent(HalApiInterface.class))
          .collect(Collectors.toList());

      if (resourceFields.isEmpty()) {
        return Optional.<Field>empty();
      }

      if (resourceFields.size() > 1) {
        throw new HalApiDeveloperException("More than one field annotated with @" + HalApiInterface.class.getSimpleName() + " was found: " + resourceFields);
      }

      return Optional.of(resourceFields.get(0));
    })
        .orElseThrow(() -> new HalApiDeveloperException("No test class was found for " + context));
  }

  private static SpringRhymeIntegrationTest getAnnotation(ExtensionContext context) {

    return context.getTestClass().map(testClass -> {

      SpringRhymeIntegrationTest testAnnotation = testClass.getAnnotation(SpringRhymeIntegrationTest.class);
      if (testAnnotation == null) {
        throw new HalApiDeveloperException(testClass + " is not annotated with " + SpringRhymeIntegrationTest.class);
      }

      return testAnnotation;
    })
        .orElseThrow(() -> new HalApiDeveloperException("No test class was found for " + context));
  }

  private static String getEntryPointUri(ExtensionContext context) {

    SpringRhymeIntegrationTest testAnnotation = getAnnotation(context);

    String entryPointUri = testAnnotation.entryPointUri();
    if (StringUtils.isBlank(entryPointUri)) {
      throw new HalApiDeveloperException("entryPointUri attribute is missing for " + SpringRhymeIntegrationTest.class);
    }

    return entryPointUri;
  }

  private static Class getApplicationClass(ExtensionContext context) {

    SpringRhymeIntegrationTest testAnnotation = getAnnotation(context);

    Class<?> applicationClass = testAnnotation.applicationClass();
    if (applicationClass == null) {
      throw new HalApiDeveloperException("applicationClass attribute is missing for " + SpringRhymeIntegrationTest.class);
    }

    return applicationClass;
  }

  private static class ApplicationMainThread extends Thread {

    private final Class<?> applicationClass;
    private final String[] args;

    private RuntimeException startupError;

    ApplicationMainThread(Class<?> applicationClass, String... args) {
      this.applicationClass = applicationClass;
      this.args = args;
    }

    @Override
    public void run() {
      try {
        Method mainMethod = applicationClass.getMethod("main", String[].class);

        mainMethod.invoke(null, new Object[] { args });
      }
      catch (Throwable ex) {
        startupError = new RuntimeException("Failed to execute " + applicationClass.getName() + "#main", ex);
      }
    }

    private void waitUntilEntryPointCanBeLoaded(String entryPointUri) {

      await()
          .atMost(Duration.ofSeconds(30))
          .alias("Spring Boot application responds to HTTP request")
          .untilAsserted(() -> assertThatEntryPointCanBeLoaded(entryPointUri));
    }

    private void assertThatEntryPointCanBeLoaded(String entryPointUri) {

      if (startupError != null) {
        throw startupError;
      }

      try {
        HttpURLConnection connection = (HttpURLConnection)URI.create(entryPointUri).toURL().openConnection();
        connection.connect();

        assertThat(connection.getResponseCode())
            .as("Response code of " + entryPointUri)
            .isEqualTo(200);
      }
      catch (IOException ex) {
        fail("Failed to load " + entryPointUri, ex);
      }
    }
  }
}
