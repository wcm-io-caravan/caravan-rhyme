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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.server.RhymeMetadataConfiguration;
import io.wcm.caravan.rhyme.api.spi.HalResourceLoader;
import io.wcm.caravan.rhyme.impl.RhymeImpl;

@State(Scope.Benchmark)
public class RhymeState {

  private static final Logger log = LoggerFactory.getLogger(RhymeState.class);

  private Rhyme lastRhymeInstance;

  @TearDown(Level.Trial)
  public void tearDown() {
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

    StringWriter writer = new StringWriter();
    try (JsonGenerator generator = factory.createGenerator(writer)) {
      generator.writeTree(metadata.getModel());

      log.info("Rhyme performance metadata of last measurement:\n{}", writer);
    }
    catch (IOException ex) {
      throw new UncheckedIOException("Failed to write metadata to console", ex);
    }
  }

  Rhyme createRhyme(HalResourceLoader loader, MetricsToggle metrics) {

    lastRhymeInstance = RhymeBuilder.withResourceLoader(loader)
        .withMetadataConfiguration(new RhymeMetadataConfiguration() {

          @Override
          public boolean isMetadataGenerationEnabled() {
            return metrics == MetricsToggle.ENABLED;
          }

        }).buildForRequestTo("/foo");

    return lastRhymeInstance;
  }

  enum MetricsToggle {
    ENABLED, DISABLED
  }
}
