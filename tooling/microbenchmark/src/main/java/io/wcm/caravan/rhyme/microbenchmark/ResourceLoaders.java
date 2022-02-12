package io.wcm.caravan.rhyme.microbenchmark;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.google.common.collect.ImmutableMap;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

public class ResourceLoaders {

  private static NettyHttpServer nettyServer;

  static void init() {
    nettyServer = new NettyHttpServer();
    nettyServer.start();
  }

  static void tearDown() {
    if (nettyServer != null) {
      nettyServer.shutdown();
    }
  }

  static HalResourceLoader preBuilt() {

    return new HalResourceLoader() {

      private final HalResponse response = RhymeBuilder.create()
          .buildForRequestTo("/foo")
          .renderResponse(new DynamicResourceImpl())
          .blockingGet();

      @Override
      public Single<HalResponse> getHalResource(String uri) {
        return Single.just(response);
      }
    };
  }

  static HalResourceLoader parsing() {

    return HalResourceLoader.create(new HttpClientSupport() {

      private final byte[] bytes = getJsonResponse().getBytes(StandardCharsets.UTF_8);

      @Override
      public void executeGetRequest(URI uri, HttpClientCallback callback) {

        callback.onHeadersAvailable(200, ImmutableMap.of());
        callback.onBodyAvailable(new ByteArrayInputStream(bytes));
      }
    });
  }

  static HalResourceLoader network() {

    HalResourceLoader loader = HalResourceLoader.create();

    return path -> loader.getHalResource("http://localhost:" + nettyServer.getPort());
  }

  static String getJsonResponse() {
    return preBuilt().getHalResource("/foo").blockingGet().getBody().getModel().toString();
  }
}
