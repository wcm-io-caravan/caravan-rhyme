package io.wcm.caravan.rhyme.testing.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URI;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import io.wcm.caravan.rhyme.api.exceptions.HalApiClientException;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;


public class ApacheAsyncHttpTest extends AbstractHalResourceLoaderTest {

  @Override
  protected HalResourceLoader createLoaderUnderTest() {

    return HalResourceLoader.create(new ApacheAsyncHttpSupport());
  }

  @Test
  public void should_use_baseUri() {

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
