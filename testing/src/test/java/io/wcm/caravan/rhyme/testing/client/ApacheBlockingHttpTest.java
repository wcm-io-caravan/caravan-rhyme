package io.wcm.caravan.rhyme.testing.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URI;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;


public class ApacheBlockingHttpTest extends AbstractHalResourceLoaderTest {

  @Override
  protected HalResourceLoader createLoaderUnderTest() {

    return HalResourceLoader.builder()
        .withCustomHttpClient(new ApacheBlockingHttpSupport())
        .build();
  }

  @Test
  public void should_use_baseUri() {

    URI baseUri = URI.create("https://foo.bar");

    ApacheBlockingHttpSupport support = new ApacheBlockingHttpSupport(baseUri);

    HalResourceLoader loader = HalResourceLoader.builder().withCustomHttpClient(support).build();

    Throwable ex = catchThrowable(() -> loader.getHalResource("/").blockingGet());

    assertThat(ex)
        .isInstanceOf(HalApiClientException.class);

    assertThat(((HalApiClientException)ex).getRequestUrl())
        .isEqualTo("https://foo.bar/");

    assertThat(ex)
        .hasRootCauseInstanceOf(UnknownHostException.class);
  }

}
