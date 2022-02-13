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
import java.io.IOException;
import java.io.UncheckedIOException;
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.client.HalResourceLoaderBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.server.RhymeMetadataConfiguration;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.RhymeImpl;
import io.wcm.caravan.rhyme.microbenchmark.resources.DynamicResourceImpl;
import io.wcm.caravan.rhyme.microbenchmark.resources.StatePojo;
import io.wcm.caravan.rhyme.microbenchmark.server.NettyHttpServer;

@State(Scope.Benchmark)
public class RhymeState {

  private StatePojo firstResponseState;

  HalResourceLoader preBuiltLoader;

  private Map<String, byte[]> preBuiltResponseBytes;

  HalResourceLoader parsingLoader;

  private NettyHttpServer nettyServer;

  HalResourceLoader networkLoader;

  HalResourceLoader cachingLoader;


  private Rhyme lastRhymeInstance;

  @Setup(Level.Trial)
  public void init() {

    firstResponseState = StatePojo.createTestState();

    Stream<String> allPaths = Stream.concat(Stream.of("/"), IntStream.range(0, NUM_LINKED_RESOURCES).mapToObj(i -> "/" + i));

    Map<String, HalResponse> preBuiltResponses = allPaths
        .map(this::renderResponse)
        .collect(Collectors.toMap(HalResponse::getUri, Function.identity(), (v1, v2) -> v1, LinkedHashMap::new));

    preBuiltLoader = uri -> Single.just(preBuiltResponses.get(uri));

    preBuiltResponseBytes = new HashMap<>();
    preBuiltResponses.forEach((uri, response) -> preBuiltResponseBytes.put(uri, response.getBody().getModel().toString().getBytes(StandardCharsets.UTF_8)));

    parsingLoader = HalResourceLoader.create((uri, callback) -> {

      callback.onHeadersAvailable(200, ImmutableMap.of());
      callback.onBodyAvailable(new ByteArrayInputStream(preBuiltResponseBytes.get(uri.toString())));
    });


    nettyServer = new NettyHttpServer(preBuiltResponseBytes);
    nettyServer.start();

    networkLoader = new HalResourceLoader() {

      private final HalResourceLoader loader = HalResourceLoader.create();

      @Override
      public Single<HalResponse> getHalResource(String uri) {
        return loader.getHalResource("http://localhost:" + nettyServer.getPort() + uri);
      }
    };

    cachingLoader = HalResourceLoaderBuilder.create()
        .withExistingLoader(parsingLoader)
        .withMemoryCache()
        .build();
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

    if (lastRhymeInstance != null) {
      RequestMetricsCollector metrics = ((RhymeImpl)lastRhymeInstance).getMetrics();
      HalResource metadata = metrics.createMetadataResource(null);
      if (metadata != null) {

        writeMetadataToConsole(metadata);
      }
    }
  }

  protected void writeMetadataToConsole(HalResource metadata) {

    DefaultIndenter indenter = new DefaultIndenter("  ", "\n");

    ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setDefaultPrettyPrinter(new DefaultPrettyPrinter()
            .withArrayIndenter(indenter));

    JsonFactory factory = new JsonFactory(mapper);

    System.err.println("Rhyme performance metadata of last measurement:");

    try (JsonGenerator generator = factory.createGenerator(System.err)) {
      generator.writeTree(metadata.getModel());
    }
    catch (IOException ex) {
      throw new UncheckedIOException("Failed to write metadata to console", ex);
    }
  }

  public byte[] getFirstResponseBytes() {
    return preBuiltResponseBytes.values().iterator().next();
  }

  Rhyme createRhyme(HalResourceLoader loader, Metrics metrics) {

    lastRhymeInstance = RhymeBuilder.withResourceLoader(loader)
        .withMetadataConfiguration(new RhymeMetadataConfiguration() {

          @Override
          public boolean isMetadataGenerationEnabled() {
            return metrics == Metrics.ENABLED;
          }

        }).buildForRequestTo("/foo");

    return lastRhymeInstance;
  }

  enum Metrics {
    ENABLED, DISABLED
  }

  public int getNettyPort() {
    return NettyHttpServer.NETTY_PORT_NR;
  }

  public StatePojo getFirstResponseState() {
    return firstResponseState;
  }
}
