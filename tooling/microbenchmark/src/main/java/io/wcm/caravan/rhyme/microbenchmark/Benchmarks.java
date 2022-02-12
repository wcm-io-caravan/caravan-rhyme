/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.wcm.caravan.rhyme.microbenchmark;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openjdk.jmh.annotations.Mode.AverageTime;

import java.io.IOException;
import java.util.function.Supplier;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableList;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.HalResponse;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.RhymeMetadataConfiguration;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.RhymeImpl;

@BenchmarkMode(AverageTime)
@OutputTimeUnit(MILLISECONDS)
@Fork(value = 2, warmups = 0)
@Warmup(iterations = 3, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = SECONDS)
@State(Scope.Benchmark)
public class Benchmarks {

  private static HalResourceLoader preBuiltLoader = ResourceLoaders.preBuilt();
  private static HalResourceLoader parsingLoader = ResourceLoaders.parsing();
  private static HalResourceLoader networkLoader = ResourceLoaders.network();

  private Rhyme lastRhymeInstance;

  @Setup(Level.Trial)
  public void init() {
    ResourceLoaders.init();
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    ResourceLoaders.tearDown();

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
      throw new RuntimeException("Failed to write metadata to console", ex);
    }
  }

  private Rhyme createRhyme(HalResourceLoader loader, Metrics metrics) {

    lastRhymeInstance = RhymeBuilder.withResourceLoader(loader)
        .withMetadataConfiguration(new RhymeMetadataConfiguration() {

          @Override
          public boolean isMetadataGenerationEnabled() {
            return metrics == Metrics.ENABLED;
          }

        }).buildForRequestTo("/foo");

    return lastRhymeInstance;
  }

  HalResponse render(Supplier<LinkableResource> constructor, Metrics metrics) {

    Rhyme rhyme = createRhyme(HalResourceLoader.create(), metrics);

    return rhyme.renderResponse(constructor.get()).blockingGet();
  }

  @Benchmark
  public HalResponse render() {

    return render(StaticResourceImpl::new, Metrics.DISABLED);
  }

  @Benchmark
  public HalResponse renderWithMetrics() {

    return render(StaticResourceImpl::new, Metrics.ENABLED);
  }

  @Benchmark
  public HalResponse renderWithInstanceCreation() {

    return render(DynamicResourceImpl::new, Metrics.DISABLED);
  }

  @Benchmark
  public HalResponse renderWithObjectMapping() {

    return render(MappingResourceImpl::new, Metrics.DISABLED);
  }

  private ImmutableList<Object> callClientMethods(HalResourceLoader loader, Metrics metrics) {

    Rhyme rhyme = createRhyme(loader, metrics);

    Resource clientProxy = rhyme.getRemoteResource("/foo", Resource.class);

    return ImmutableList.of(clientProxy.getState().blockingGet(), clientProxy.createLink(),
        clientProxy.getLinked1().map(l -> l.getState()).toList().blockingGet(),
        clientProxy.getLinked1().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked2().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked3().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked4().map(l -> l.createLink().getHref()).toList().blockingGet(),
        clientProxy.getLinked5().map(l -> l.createLink().getHref()).toList().blockingGet());
  }

  @Benchmark
  public Object client() {
    return callClientMethods(preBuiltLoader, Metrics.DISABLED);
  }

  @Benchmark
  public Object clientWithMetrics() {
    return callClientMethods(preBuiltLoader, Metrics.ENABLED);
  }

  @Benchmark
  public Object clientWithParsing() {
    return callClientMethods(parsingLoader, Metrics.DISABLED);
  }

  @Benchmark
  public Object clientWithNetworkOverhead() {
    return callClientMethods(networkLoader, Metrics.DISABLED);
  }

  enum Metrics {
    ENABLED, DISABLED
  }
}
