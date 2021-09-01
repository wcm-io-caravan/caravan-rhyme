package io.wcm.caravan.ryhme.testing.client;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;

public class HalApiClientExtension implements ParameterResolver {

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(HalApiClient.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {

    HalResourceLoader loader = HalResourceLoader.withCustomHttpClient(new ApacheAsyncHttpSupport());

    RequestMetricsCollector metrics = RequestMetricsCollector.create();

    return HalApiClient.create(loader, metrics);
  }

}
