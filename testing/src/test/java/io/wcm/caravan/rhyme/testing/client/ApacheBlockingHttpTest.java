package io.wcm.caravan.rhyme.testing.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;


class ApacheBlockingHttpTest extends AbstractHalResourceLoaderTest {

  @Override
  protected HalResourceLoader createLoaderUnderTest() {

    return HalResourceLoader.create(new ApacheBlockingHttpSupport());
  }

  @Test
  void should_use_custom_HttpClient_instance() throws IOException {

    CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);

    HalResourceLoader loader = HalResourceLoader.create(new ApacheBlockingHttpSupport(client));

    IOException rootCause = new IOException("failed");

    when(client.execute(any(HttpUriRequest.class)))
        .thenThrow(rootCause);

    Throwable ex = catchThrowable(() -> loader.getHalResource("/foo").blockingGet());

    assertThat(ex)
        .isNotNull()
        .hasRootCause(rootCause);
  }

  @Test
  void should_use_baseUri() {

    URI baseUri = URI.create("https://foo.bar");

    ApacheBlockingHttpSupport support = new ApacheBlockingHttpSupport(baseUri);

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
