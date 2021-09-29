package io.wcm.caravan.rhyme.osgi.it.extensions;

import static io.wcm.caravan.rhyme.osgi.it.IntegrationTestEnvironment.ENTRY_POINT_URL;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.osgi.it.IntegrationTestEnvironment;

public class WaitForServerStartupExtension implements BeforeAllCallback, InvocationInterceptor {

  private static final Logger log = LoggerFactory.getLogger(WaitForServerStartupExtension.class);

  private static final Namespace NAMESPACE = Namespace.create(WaitForServerStartupExtension.class);

  private static final String LAST_CAUGHT_EXCEPTION = "lastCaughtException";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final JsonFactory JSON_FACTORY = new JsonFactory(OBJECT_MAPPER);

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {

    Store store = context.getStore(NAMESPACE);

    Throwable lastCaughtException = store.get(LAST_CAUGHT_EXCEPTION, Throwable.class);
    if (lastCaughtException != null) {
      throw new IllegalStateException("Failed to wait for server startup for previous test, and will not try again.", lastCaughtException);
    }

    int maxWaitSeconds = 30;
    Stopwatch sw = Stopwatch.createStarted();

    while (sw.elapsed(TimeUnit.SECONDS) < maxWaitSeconds) {
      try {
        assertThatEntryPointOfExampleServiceIsAvailable(ENTRY_POINT_URL);
        return;
      }
      catch (Exception | AssertionError e) {
        lastCaughtException = e;
        log.warn("The Sling Launchpad doesn't seem to have started completely yet. A " + e.getClass().getSimpleName() + " was caught: "
            + e.getMessage());
        try {
          Thread.sleep(1000);
        }
        catch (InterruptedException e1) {
          log.error("Thread was interrupted while waiting for entry point to become available", e1);
        }
      }
    }

    store.put(LAST_CAUGHT_EXCEPTION, lastCaughtException);

    throw new IllegalStateException("Entry point at " + IntegrationTestEnvironment.ENTRY_POINT_URL
        + " still fails to load after waiting " + maxWaitSeconds + " seconds."
        + " Possible reasons are that not all OSGI bundles could be started or that you have a different server running on "
        + ENTRY_POINT_URL, lastCaughtException);
  }

  private static HalResource assertThatEntryPointOfExampleServiceIsAvailable(String url) throws IOException {

    HttpResponse response = getResponse(url);

    assertThat(response.getStatusLine().getStatusCode()).as("Response code for " + url)
        .isEqualTo(HttpServletResponse.SC_OK);

    assertThat(response.getFirstHeader("Content-Type").getValue()).as("Content type of " + url)
        .isEqualTo(HalResource.CONTENT_TYPE);

    String jsonString = EntityUtils.toString(response.getEntity());
    assertThat(jsonString).as("JSON response")
        .isNotBlank();

    JsonNode jsonNode = JSON_FACTORY.createParser(jsonString).readValueAsTree();
    HalResource halResource = new HalResource(jsonNode);

    assertThat(halResource.getLink()).as("self link of " + url)
        .isNotNull();

    assertThat(halResource.getLink().getHref()).as("self link URL")
        .isEqualTo("/");

    assertThat(halResource.getLink().getTitle()).as("self link title")
        .startsWith("The HAL API entry point of the OSGi/JAX-RS example service");

    return halResource;
  }

  private static HttpResponse getResponse(String fullUrl) throws IOException, ClientProtocolException {

    HttpGet get = new HttpGet(fullUrl);

    CloseableHttpClient client = HttpClientBuilder.create().build();

    return client.execute(get);
  }
}
