package io.wcm.caravan.rhyme.microbenchmark;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.api.spi.HttpClientCallback;
import io.wcm.caravan.rhyme.api.spi.HttpClientSupport;

public class ResourceLoaders {

  private static NettyHttpServer nettyServer;

  private static Map<String, HalResponse> preBuiltResponses;

  private static Map<String, byte[]> preBuiltResponseBytes;

  static void init() {

    preBuiltResponses = Stream.concat(Stream.of("/"), IntStream.range(0, ResourceParameters.numLinkedResource()).mapToObj(i -> "/" + i))
        .map(path -> RhymeBuilder.create()
            .buildForRequestTo(path)
            .renderResponse(new DynamicResourceImpl(path))
            .blockingGet()
            .withMaxAge(3600))
        .collect(Collectors.toMap(HalResponse::getUri, Function.identity()));

    preBuiltResponseBytes = new HashMap<>();
    preBuiltResponses.forEach((uri, response) -> preBuiltResponseBytes.put(uri, response.getBody().getModel().toString().getBytes(StandardCharsets.UTF_8)));

    nettyServer = new NettyHttpServer(preBuiltResponseBytes);
    nettyServer.start();
  }

  static void tearDown() {
    if (nettyServer != null) {
      nettyServer.shutdown();
    }
  }

  static HalResourceLoader preBuilt() {

    return new HalResourceLoader() {

      @Override
      public Single<HalResponse> getHalResource(String uri) {
        return Single.just(preBuiltResponses.get(uri));
      }
    };
  }

  static HalResourceLoader parsing() {

    return HalResourceLoader.create(new HttpClientSupport() {

      @Override
      public void executeGetRequest(URI uri, HttpClientCallback callback) {

        callback.onHeadersAvailable(200, ImmutableMap.of());
        callback.onBodyAvailable(new ByteArrayInputStream(preBuiltResponseBytes.get(uri.toString())));
      }
    });
  }

  static HalResourceLoader network() {

    HalResourceLoader loader = HalResourceLoader.create();

    return path -> loader.getHalResource("http://localhost:" + nettyServer.getPort() + path);
  }
}
