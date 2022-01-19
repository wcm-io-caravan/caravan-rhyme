package io.wcm.caravan.rhyme.testing.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;


class ApacheAsyncHttpTest extends AbstractHalResourceLoaderTest {

  @Override
  protected HalResourceLoader createLoaderUnderTest() {

    return HalResourceLoader.create(new ApacheAsyncHttpSupport());
  }

  @Test
  void should_use_and_start_custom_HttpClient_instance() {

    CloseableHttpAsyncClient client = Mockito.mock(CloseableHttpAsyncClient.class);

    HalResourceLoader loader = HalResourceLoader.create(new ApacheAsyncHttpSupport(client));

    verify(client).isRunning();
    verify(client).start();

    RuntimeException rootCause = new RuntimeException("failed");

    when(client.execute(any(HttpUriRequest.class), any()))
        .thenThrow(rootCause);

    Throwable ex = catchThrowable(() -> loader.getHalResource("/foo").blockingGet());

    assertThat(ex)
        .isNotNull()
        .hasRootCause(rootCause);
  }

  @Test
  void should_not_start_HttpClient_instance_if_already_started() throws IOException {

    CloseableHttpAsyncClient client = Mockito.mock(CloseableHttpAsyncClient.class);

    when(client.isRunning())
        .thenReturn(true);

    new ApacheAsyncHttpSupport(client);

    verify(client, never())
        .start();
  }

  @Test
  void should_use_baseUri() {

    URI baseUri = URI.create("https://foo.bar");

    ApacheAsyncHttpSupport support = new ApacheAsyncHttpSupport(baseUri);

    HalResourceLoader loader = HalResourceLoader.create(support);

    Throwable ex = catchThrowable(() -> loader.getHalResource("/").blockingGet());

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class);

    assertThat(((HalApiClientException)ex).getRequestUrl())
        .isEqualTo("https://foo.bar/");

    assertThat(ex)
        .hasRootCauseInstanceOf(UnknownHostException.class);
  }

}
