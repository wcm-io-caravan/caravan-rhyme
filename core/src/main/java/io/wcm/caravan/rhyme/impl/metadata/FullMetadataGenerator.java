/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2021 wcm.io
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
package io.wcm.caravan.rhyme.impl.metadata;

import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.EMISSION_TIMES;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.INVOCATION_TIMES;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.MAX_AGE;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.PROXY_TIMES;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.RENDERING_TIMES;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.RESPONSE_TIMES;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.SLING_MODELS;
import static io.wcm.caravan.rhyme.impl.metadata.ResponseMetadataRelations.SOURCE_LINKS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.client.HalApiClient;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.relations.StandardRelations;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.api.server.AsyncHalResponseRenderer;

/**
 * Full implementation of {@link RequestMetricsCollector} that keeps track of all upstream resources that have been
 * retrieved, and additional invocation/emission times to analyze the performance of a request
 */
public class FullMetadataGenerator extends MaxAgeOnlyCollector implements RequestMetricsCollector {

  private static final Logger log = LoggerFactory.getLogger(FullMetadataGenerator.class);

  private final Stopwatch overalResponseTimeStopwatch = Stopwatch.createStarted();

  private static final Map<TimeUnit, String> TIME_UNIT_ABBRS = ImmutableMap.of(
      MINUTES, "m",
      SECONDS, "s",
      MILLISECONDS, "ms",
      MICROSECONDS, "μs",
      NANOSECONDS, "ns");

  private final List<Link> sourceLinks = Collections.synchronizedList(new ArrayList<>());

  private final List<TimeMeasurement> inputMaxAges = Collections.synchronizedList(new ArrayList<>());
  private final List<TimeMeasurement> inputResponseTimes = Collections.synchronizedList(new ArrayList<>());
  private final ListMultimap<String, TimeMeasurement> methodInvocationTimes = Multimaps.synchronizedListMultimap(LinkedListMultimap.create());

  private final AtomicLong metricsCollectionNanos = new AtomicLong();

  private final AtomicBoolean metadataWasRendered = new AtomicBoolean();

  @Override
  public void onResponseRetrieved(String resourceUri, String resourceTitle, Integer maxAgeSeconds, long responseTimeMicros) {

    super.onResponseRetrieved(resourceUri, resourceTitle, maxAgeSeconds, responseTimeMicros);

    if (metadataWasRendered.get()) {
      log.warn("Response from {} was retrieved after embedded metadata resource was rendered", resourceUri);
      return;
    }

    if (maxAgeSeconds != null) {
      inputMaxAges.add(new TimeMeasurement(resourceUri, maxAgeSeconds, SECONDS));
    }
    inputResponseTimes.add(new TimeMeasurement(resourceUri, responseTimeMicros, MICROSECONDS));

    Link link = new Link(resourceUri);
    link.setTitle(resourceTitle);
    sourceLinks.add(link);
  }

  @Override
  public RequestMetricsStopwatch startStopwatch(Class measuringClass, Supplier<String> taskDescription) {

    return new RequestMetricsStopwatch() {

      private final Stopwatch stopwatch = Stopwatch.createStarted();

      @Override
      public void close() {
        rememberInvocationTimes(measuringClass, taskDescription, stopwatch.elapsed(NANOSECONDS));
      }
    };
  }

  @Override
  public void onMethodInvocationFinished(Class category, String methodDescription, long invocationDurationMicros) {

    rememberInvocationTimes(category, () -> methodDescription, MICROSECONDS.toNanos(invocationDurationMicros));
  }

  private void rememberInvocationTimes(Class<?> category, Supplier<String> methodDescription, long invocationDurationNanos) {

    Stopwatch sw = Stopwatch.createStarted();

    methodInvocationTimes.put(category.getSimpleName(),
        new TimeMeasurement(methodDescription.get(), invocationDurationNanos));

    metricsCollectionNanos.addAndGet(sw.elapsed(NANOSECONDS));
  }

  List<TimeMeasurement> getSortedInputResponseTimes() {
    return TimeMeasurement.LONGEST_TIME_FIRST.sortedCopy(inputResponseTimes);
  }

  List<TimeMeasurement> getGroupedAndSortedInvocationTimes(String simpleClassName, boolean useMax) {

    List<TimeMeasurement> invocationTimes = methodInvocationTimes.get(simpleClassName);

    List<TimeMeasurement> groupedInvocationTimes = new ArrayList<>();
    invocationTimes.stream()
        .collect(Collectors.groupingBy(TimeMeasurement::getText))
        .forEach((text, measurements) -> {
          LongStream individualTimes = measurements.stream().mapToLong(TimeMeasurement::getNanos);

          long totalNanos = useMax ? individualTimes.max().orElse(0) : individualTimes.sum();
          long invocations = measurements.stream().count();

          String prefix = "";
          if (measurements.size() > 1) {
            prefix = useMax ? "max of " : "sum of ";
          }
          groupedInvocationTimes.add(new TimeMeasurement(prefix + invocations + "x " + text, totalNanos));
        });

    groupedInvocationTimes.sort(TimeMeasurement.LONGEST_TIME_FIRST);

    return groupedInvocationTimes;
  }

  List<TimeMeasurement> getSortedInputMaxAgeSeconds() {
    return TimeMeasurement.LONGEST_TIME_FIRST.sortedCopy(inputMaxAges);
  }

  float getOverallResponseTimeMillis() {
    return overalResponseTimeStopwatch.elapsed(MICROSECONDS) / 1000.f;
  }

  float getSumOfResponseTimeMillis() {
    return inputResponseTimes.stream()
        .mapToLong(TimeMeasurement::getNanos)
        .sum() / 1.0e+6f;
  }

  float getSumOfInvocationMillis(Class category) {
    return methodInvocationTimes.get(category.getSimpleName()).stream()
        .mapToLong(TimeMeasurement::getNanos)
        .sum() / 1.0e+6f;
  }

  List<Link> getSourceLinks() {
    return ImmutableList.copyOf(sourceLinks);
  }

  /**
   * @param resourceImpl the resource implementation that was used to generate the resource
   * @return a new {@link HalResource} instance with detailed information about timing and caching for all upstream
   *         resources accessed while handling the current request
   */
  @Override
  public HalResource createMetadataResource(LinkableResource resourceImpl) {

    super.createMetadataResource(resourceImpl);

    metadataWasRendered.set(true);

    Stopwatch stopwatch = Stopwatch.createStarted();

    HalResource metadataResource = new HalResource();

    metadataResource.getModel().put("title", "Detailed information about the performance and input data for this request");
    if (resourceImpl != null) {
      metadataResource.getModel().put("class", resourceImpl.getClass().getName());
    }

    HalResource viaLinks = new HalResource().addLinks(StandardRelations.VIA, getSourceLinks());
    addEmbedded(metadataResource, SOURCE_LINKS, viaLinks,
        "Links to all requested upstream HAL resources (in the order the responses have been retrieved)",
        "If you see lots of untitled resources here then free feel to add a title to the self link in that resource in the upstream service.");

    HalResource responseTimes = createTimingResource(getSortedInputResponseTimes(), MILLISECONDS);
    addEmbedded(metadataResource, RESPONSE_TIMES, responseTimes,
        "The individual response & parse times of all retrieved HAL resources",
        "Response times > ~20ms usually indicate that the resource was not found in cache"
            + " - a reload of this resource should then be much faster. "
            + "If you see many individual requests here then check if the upstream "
            + "service also provides a way to fetch this data all at once. ");

    HalResource maxAgeResource = createTimingResource(getSortedInputMaxAgeSeconds(), SECONDS);
    addEmbedded(metadataResource, MAX_AGE, maxAgeResource,
        "The max-age cache header values of all retrieved resources",
        "If the max-age in this response's cache headers is lower then you expected, "
            + "then check the resources at the very bottom of the list, because they will determine the overall max-age time.");

    List<TimingResourceCategory> allCategories = getAllCategories();

    allCategories.forEach(category -> createAndEmbed(metadataResource, category, MILLISECONDS));

    // and also include the overall max-age of the response
    metadataResource.getModel().put("maxAge", getResponseMaxAge() + " s");

    // and a summary of the important timing results
    metadataResource.getModel().put("sumOfProxyInvocationTime", getSumOfInvocationMillis(HalApiClient.class) + "ms");
    metadataResource.getModel().put("sumOfResourceAssemblyTime", getSumOfInvocationMillis(AsyncHalResponseRenderer.class) + "ms");
    metadataResource.getModel().put("sumOfResponseAndParseTimes", getSumOfResponseTimeMillis() + "ms");
    metadataResource.getModel().put("overallServerSideResponseTime", getOverallResponseTimeMillis() + "ms");
    metadataResource.getModel().put("metricsCollectionTime", NANOSECONDS.toMillis(metricsCollectionNanos.get()) + "ms");
    metadataResource.getModel().put("metadataGenerationTime", stopwatch.elapsed(MILLISECONDS) + "ms");

    return metadataResource;
  }

  private List<TimingResourceCategory> getAllCategories() {

    List<TimingResourceCategory> knownCategories = getKnownCategoriesWithDescription();

    List<String> knownClassNames = knownCategories.stream()
        .map(category -> category.simpleClassName)
        .collect(Collectors.toList());

    List<TimingResourceCategory> allCategories = new ArrayList<>(knownCategories);

    methodInvocationTimes.keySet().stream()
        .filter(className -> !knownClassNames.contains(className))
        .map(className -> new TimingResourceCategory(className, INVOCATION_TIMES, "Invocations of methods in class " + className, null, false))
        .forEach(allCategories::add);

    return allCategories;
  }

  private ImmutableList<TimingResourceCategory> getKnownCategoriesWithDescription() {
    return ImmutableList.of(

        new TimingResourceCategory(EmissionStopwatch.class.getSimpleName(), EMISSION_TIMES,
            "A breakdown of emission and rendering times by resource and method",
            "Use these times to identify performance hotspots in your server-side implementation classes", true),

        new TimingResourceCategory(HalApiClient.class.getSimpleName(), PROXY_TIMES,
            "A breakdown of time spent in blocking HalApiClient proxy method calls",
            null, false),

        new TimingResourceCategory(AsyncHalResponseRenderer.class.getSimpleName(), RENDERING_TIMES,
            "A breakdown of time spent in blocking method calls while rendering resources",
            null, false),

        new TimingResourceCategory("SlingRhymeImpl", SLING_MODELS,
            "A breakdown of time spent adapting sling models from SlingRhyme",
            null, false));
  }


  static class TimingResourceCategory {

    private final String simpleClassName;
    private final String relation;

    private final String description;
    private final String developerHint;
    private final boolean useMaxForAggregration;

    TimingResourceCategory(String simpleClassName, String relation, String description, String developerHint, boolean useMaxForAggregration) {
      this.simpleClassName = simpleClassName;
      this.relation = relation;
      this.description = description;
      this.developerHint = developerHint;
      this.useMaxForAggregration = useMaxForAggregration;
    }
  }

  private void createAndEmbed(HalResource metadataResource, TimingResourceCategory category, TimeUnit unit) {

    HalResource timingResource = createTimingResource(getGroupedAndSortedInvocationTimes(category.simpleClassName, category.useMaxForAggregration), unit);

    addEmbedded(metadataResource, category.relation, timingResource, category.description, category.developerHint);
  }

  private static void addEmbedded(HalResource metadataResource, String relation, HalResource toEmbed, String title, String developerHint) {

    if (toEmbed.getModel().path("measurements").size() == 0 && toEmbed.getLinks().isEmpty()) {
      return;
    }

    toEmbed.getModel().put("title", title);

    if (developerHint != null) {
      toEmbed.getModel().put("developerHint", developerHint);
    }

    metadataResource.addEmbedded(relation, toEmbed);
  }

  private HalResource createTimingResource(List<TimeMeasurement> list, TimeUnit unit) {

    ObjectNode model = JsonNodeFactory.instance.objectNode();
    ArrayNode individualMetrics = model.putArray("measurements");

    list.stream()
        .map(measurement -> formatFloat(measurement.getTime(unit)) + " " + TIME_UNIT_ABBRS.get(unit) + " - " + measurement.getText())
        .forEach(individualMetrics::add);

    return new HalResource(model);
  }

  private static String formatFloat(float value) {
    // this is slightly faster then creating a DecimalFormat with pattern "0.0##"
    return Float.toString(Math.round(value * 1000) / 1000.f);
  }

  /**
   * Composition of a time value with unit and description
   */
  public static class TimeMeasurement {

    static final Ordering<TimeMeasurement> LONGEST_TIME_FIRST = Ordering.natural().onResultOf(TimeMeasurement::getNanos).reverse();

    private final String text;
    private final Long nanos;

    TimeMeasurement(String text, long nanos) {
      this.text = text;
      this.nanos = nanos;
    }

    TimeMeasurement(String text, long duration, TimeUnit unit) {
      this.text = text;
      this.nanos = unit.toNanos(duration);
    }

    public String getText() {
      return this.text;
    }

    public Long getNanos() {
      return this.nanos;
    }

    public float getTime(TimeUnit unit) {
      return unit.convert(nanos * 1000, NANOSECONDS) / 1000.f;
    }
  }
}
