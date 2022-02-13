/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2022 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.rhyme.microbenchmark;

import static io.wcm.caravan.rhyme.microbenchmark.resources.ResourceParameters.NUM_LINKED_RESOURCES;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.microbenchmark.resources.BenchmarkResourceState;
import io.wcm.caravan.rhyme.microbenchmark.resources.DynamicResourceImpl;
import io.wcm.caravan.rhyme.microbenchmark.server.NettyHttpServer;

@State(Scope.Benchmark)
public class ResourceLoaders {

  HalResourceLoader preBuilt;

  private Map<String, byte[]> preBuiltResponseBytes;

  HalResourceLoader parsing;

  private NettyHttpServer nettyServer;

  HalResourceLoader network;

  HalResourceLoader caching;

  private BenchmarkResourceState firstResponseState;
  private ObjectNode firstResponseJson;


  @Setup(Level.Trial)
  public void init() {

    Stream<String> allPaths = Stream.concat(Stream.of("/"), IntStream.range(0, NUM_LINKED_RESOURCES).mapToObj(i -> "/" + i));

    Map<String, HalResponse> preBuiltResponses = allPaths
        .map(this::renderResponse)
        .collect(Collectors.toMap(HalResponse::getUri, Function.identity(), (v1, v2) -> v1, LinkedHashMap::new));

    preBuilt = uri -> Single.just(preBuiltResponses.get(uri));

    preBuiltResponseBytes = new HashMap<>();
    preBuiltResponses.forEach((uri, response) -> preBuiltResponseBytes.put(uri, response.getBody().getModel().toString().getBytes(StandardCharsets.UTF_8)));

    parsing = HalResourceLoader.create((uri, callback) -> {

      callback.onHeadersAvailable(200, ImmutableMap.of());
      callback.onBodyAvailable(new ByteArrayInputStream(preBuiltResponseBytes.get(uri.toString())));
    });


    nettyServer = new NettyHttpServer(preBuiltResponseBytes);
    nettyServer.start();

    network = new HalResourceLoader() {

      private final HalResourceLoader loader = HalResourceLoader.create();

      @Override
      public Single<HalResponse> getHalResource(String uri) {
        return loader.getHalResource("http://localhost:" + nettyServer.getPort() + uri);
      }
    };

    caching = HalResourceLoaderBuilder.create()
        .withExistingLoader(parsing)
        .withMemoryCache()
        .build();

    firstResponseState = BenchmarkResourceState.createTestState();
    firstResponseJson = BenchmarkResourceState.createMappedJson();
  }

  protected HalResponse renderResponse(String path) {

    return RhymeBuilder.create()
        .buildForRequestTo(path)
        .renderResponse(new DynamicResourceImpl(path))
        .blockingGet()
        .withMaxAge(3600);
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    if (nettyServer != null) {
      nettyServer.shutdown();
    }
  }

  public int getNettyPort() {
    return NettyHttpServer.NETTY_PORT_NR;
  }

  public byte[] getFirstResponseBytes() {
    return preBuiltResponseBytes.values().iterator().next();
  }

  public BenchmarkResourceState getFirstResponseState() {
    return firstResponseState;
  }

  public ObjectNode getFirstResponseJson() {
    return firstResponseJson;
  }
}
