/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.reha.impl.metadata;

import static io.wcm.caravan.reha.impl.metadata.ResponseMetadataRelations.EMISSION_TIMES;
import static io.wcm.caravan.reha.impl.metadata.ResponseMetadataRelations.INVOCATION_TIMES;
import static io.wcm.caravan.reha.impl.metadata.ResponseMetadataRelations.MAX_AGE;
import static io.wcm.caravan.reha.impl.metadata.ResponseMetadataRelations.RESPONSE_TIMES;
import static io.wcm.caravan.reha.impl.metadata.ResponseMetadataRelations.SOURCE_LINKS;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.apache.commons.lang3.ObjectUtils;

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
import io.wcm.caravan.reha.api.client.HalApiClient;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.relations.StandardRelations;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.api.server.AsyncHalResourceRenderer;

/**
 * Full implementation of {@link RequestMetricsCollector} that keeps track of all upstream resources that have been
 * retrieved, and additional invocation/emission times to analyze the performance of a request
 */
public class ResponseMetadataGenerator implements RequestMetricsCollector {

  private static final Map<TimeUnit, String> TIME_UNIT_ABBRS = ImmutableMap.of(
      TimeUnit.MINUTES, "m",
      TimeUnit.SECONDS, "s",
      TimeUnit.MILLISECONDS, "ms",
      TimeUnit.MICROSECONDS, "μs",
      TimeUnit.NANOSECONDS, "ns");

  private final Stopwatch overalResponseTimeStopwatch = Stopwatch.createStarted();

  private final List<TimeMeasurement> inputMaxAgeSeconds = Collections.synchronizedList(new ArrayList<>());
  private final List<TimeMeasurement> inputResponseTimes = Collections.synchronizedList(new ArrayList<>());
  private final ListMultimap<String, TimeMeasurement> methodInvocationTimes = Multimaps.synchronizedListMultimap(LinkedListMultimap.create());

  private final List<Link> sourceLinks = Collections.synchronizedList(new ArrayList<>());

  private Integer maxAgeLimit;

  @Override
  public void onResponseRetrieved(String resourceUri, String resourceTitle, Integer maxAgeSeconds, long responseTimeMicros) {

    if (maxAgeSeconds != null) {
      inputMaxAgeSeconds.add(new TimeMeasurement(resourceUri, maxAgeSeconds / 1.f, TimeUnit.SECONDS));
    }
    inputResponseTimes.add(new TimeMeasurement(resourceUri, responseTimeMicros / 1000.f, TimeUnit.MILLISECONDS));

    Link link = new Link(resourceUri);
    link.setTitle(resourceTitle);
    sourceLinks.add(link);
  }

  @Override
  public void onMethodInvocationFinished(Class category, String methodDescription, long invocationDurationMicros) {
    methodInvocationTimes.put(category.getSimpleName(),
        new TimeMeasurement(methodDescription, invocationDurationMicros / 1000.f, TimeUnit.MILLISECONDS));
  }

  @Override
  public void setResponseMaxAge(Duration duration) {
    long seconds = duration.getSeconds();
    int intSeconds = seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)seconds;
    if (maxAgeLimit != null) {
      maxAgeLimit = Math.min(maxAgeLimit, intSeconds);
    }
    else {
      maxAgeLimit = intSeconds;
    }
  }

  /**
   * @return the min max-age value of all responses that have been retrieved, or 365 days if no responses have been
   *         fetched, or none of them had a max-age header
   */
  @Override
  public Integer getResponseMaxAge() {

    if (maxAgeLimit == null && inputMaxAgeSeconds.isEmpty()) {
      return null;
    }

    int upperLimit = ObjectUtils.defaultIfNull(maxAgeLimit, (int)TimeUnit.DAYS.toSeconds(365));

    int inputMaxAge = inputMaxAgeSeconds.stream()
        // find the max-age values of all requested resources
        .mapToInt(triple -> Math.round(triple.getTime()))
        // get the minimum max age time
        .min()
        // or fall back to the upper limit if no resources were retrieved
        .orElse(upperLimit);

    return Math.min(inputMaxAge, upperLimit);
  }

  List<TimeMeasurement> getSortedInputMaxAgeSeconds() {
    return TimeMeasurement.LONGEST_TIME_FIRST.sortedCopy(inputMaxAgeSeconds);
  }

  List<TimeMeasurement> getSortedInputResponseTimes() {
    return TimeMeasurement.LONGEST_TIME_FIRST.sortedCopy(inputResponseTimes);
  }

  List<TimeMeasurement> getGroupedAndSortedInvocationTimes(Class category, boolean useMax) {

    List<TimeMeasurement> invocationTimes = methodInvocationTimes.get(category.getSimpleName());

    List<TimeMeasurement> groupedInvocationTimes = new ArrayList<>();
    invocationTimes.stream()
        .collect(Collectors.groupingBy(TimeMeasurement::getText))
        .forEach((text, measurements) -> {
          DoubleStream individualTimes = measurements.stream().mapToDouble(TimeMeasurement::getTime);

          double totalTime = useMax ? individualTimes.max().orElse(0.f) : individualTimes.sum();
          long invocations = measurements.stream().count();

          String prefix = "";
          if (measurements.size() > 1) {
            prefix = useMax ? "max of " : "sum of ";
          }
          groupedInvocationTimes.add(new TimeMeasurement(prefix + invocations + "x " + text, (float)totalTime, measurements.get(0).getUnit()));
        });

    groupedInvocationTimes.sort(TimeMeasurement.LONGEST_TIME_FIRST);

    return groupedInvocationTimes;
  }

  float getOverallResponseTimeMillis() {
    return overalResponseTimeStopwatch.elapsed(TimeUnit.MICROSECONDS) / 1000.f;
  }

  float getSumOfResponseTimeMillis() {
    return (float)inputResponseTimes.stream()
        .mapToDouble(m -> m.getTime())
        .sum();
  }

  float getSumOfInvocationMillis(Class category) {
    return (float)methodInvocationTimes.get(category.getSimpleName()).stream()
        .mapToDouble(m -> m.getTime())
        .sum();
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
    Stopwatch stopwatch = Stopwatch.createStarted();

    HalResource metadataResource = new HalResource();

    metadataResource.getModel().put("title", "Detailed information about the performance and input data for this request");
    metadataResource.getModel().put("class", resourceImpl.getClass().getName());

    HalResource viaLinks = new HalResource().addLinks(StandardRelations.VIA, getSourceLinks());
    addEmbedded(metadataResource, SOURCE_LINKS, viaLinks,
        "Links to all requested upstream HAL resources (in the order the responses have been retrieved)",
        "If you see lots of untitled resources here then free feel to add a title to the self link in that resource in the upstream service.");

    HalResource responseTimes = createTimingResource(getSortedInputResponseTimes());
    addEmbedded(metadataResource, RESPONSE_TIMES, responseTimes,
        "The individual response & parse times of all retrieved HAL resources",
        "Response times > ~20ms usually indicate that the resource was not found in cache"
            + " - a reload of this resource should then be much faster. "
            + "If you see many individual requests here then check if the upstream "
            + "service also provides a way to fetch this data all at once. ");

    HalResource emissionTimes = createTimingResource(getGroupedAndSortedInvocationTimes(EmissionStopwatch.class, true));
    addEmbedded(metadataResource, EMISSION_TIMES, emissionTimes,
        "A breakdown of emission and rendering times by resource and method",
        "Use these times to identify performance hotspots in your server-side implementation classes");

    HalResource proxyInvocationTimes = createTimingResource(getGroupedAndSortedInvocationTimes(HalApiClient.class, false));
    addEmbedded(metadataResource, INVOCATION_TIMES, proxyInvocationTimes,
        "A breakdown of time spent in blocking HalApiClient proxy method calls",
        null);

    HalResource asyncRendererResource = createTimingResource(getGroupedAndSortedInvocationTimes(AsyncHalResourceRenderer.class, false));
    addEmbedded(metadataResource, INVOCATION_TIMES, asyncRendererResource,
        "A breakdown of time spent in blocking method calls by AsyncHalResourceRenderer",
        null);

    HalResource maxAgeResource = createTimingResource(getSortedInputMaxAgeSeconds());
    addEmbedded(metadataResource, MAX_AGE, maxAgeResource,
        "The max-age cache header values of all retrieved resources",
        "If the max-age in this response's cache headers is lower then you expected, "
            + "then check the resources at the very bottom of the list, because they will determine the overall max-age time.");

    // and also include the overall max-age of the response
    metadataResource.getModel().put("maxAge", getResponseMaxAge() + " s");

    // and a summary of the important timing results
    metadataResource.getModel().put("sumOfProxyInvocationTime", getSumOfInvocationMillis(HalApiClient.class) + "ms");
    metadataResource.getModel().put("sumOfResourceAssemblyTime", getSumOfInvocationMillis(AsyncHalResourceRenderer.class) + "ms");
    metadataResource.getModel().put("sumOfResponseAndParseTimes", getSumOfResponseTimeMillis() + "ms");
    metadataResource.getModel().put("overallServerSideResponseTime", getOverallResponseTimeMillis() + "ms");
    metadataResource.getModel().put("metadataGenerationTime", stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");

    return metadataResource;
  }

  private static void addEmbedded(HalResource metadataResource, String relation, HalResource toEmbed, String title, String developerHint) {

    toEmbed.getModel().put("title", title);

    if (developerHint != null) {
      toEmbed.getModel().put("developerHint", developerHint);
    }

    metadataResource.addEmbedded(relation, toEmbed);
  }

  private static HalResource createTimingResource(List<TimeMeasurement> list) {

    ObjectNode model = JsonNodeFactory.instance.objectNode();
    ArrayNode individualMetrics = model.putArray("measurements");

    list.stream()
        .map(measurement -> measurement.getTime() + " " + TIME_UNIT_ABBRS.get(measurement.getUnit()) + " - " + measurement.getText())
        .forEach(title -> individualMetrics.add(title));

    return new HalResource(model);
  }

  /**
   * Composition of a time value with unit and description
   */
  public static class TimeMeasurement {

    static final Ordering<TimeMeasurement> LONGEST_TIME_FIRST = Ordering.natural().onResultOf(TimeMeasurement::getTime).reverse();

    private final String text;
    private final Float time;
    private final TimeUnit unit;

    TimeMeasurement(String text, Float time, TimeUnit unit) {
      this.text = text;
      this.time = time;
      this.unit = unit;
    }

    public String getText() {
      return this.text;
    }

    public Float getTime() {
      return this.time;
    }

    public TimeUnit getUnit() {
      return this.unit;
    }
  }

}
