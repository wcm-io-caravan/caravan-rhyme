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
package io.wcm.caravan.rhyme.impl.metadata;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.common.RequestMetricsStopwatch;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.impl.metadata.FullMetadataGenerator.TimeMeasurement;

/**
 * Minimal implementation of {@link RequestMetricsCollector} containing only the essential functionality required for
 * calculation of the max-age header to work
 */
public class MaxAgeOnlyCollector implements RequestMetricsCollector {

  private static final Logger log = LoggerFactory.getLogger(MaxAgeOnlyCollector.class);

  private final AtomicBoolean metadataWasRendered = new AtomicBoolean();

  private final List<TimeMeasurement> inputMaxAgeSeconds = Collections.synchronizedList(new ArrayList<>());

  private Integer maxAgeLimit;

  @Override
  public void onResponseRetrieved(String resourceUri, String resourceTitle, Integer maxAgeSeconds, long responseTimeMicros) {

    if (metadataWasRendered.get()) {
      log.warn("Response from {} was retrieved after embedded metadata resource was rendered", resourceUri);
      return;
    }

    if (maxAgeSeconds != null) {
      inputMaxAgeSeconds.add(new TimeMeasurement(resourceUri, maxAgeSeconds, SECONDS));
    }
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

    // find the max-age values of all requested resources
    int inputMaxAge = inputMaxAgeSeconds.stream()
        .mapToInt(maxAge -> (int)NANOSECONDS.toSeconds(maxAge.getNanos()))
        // get the smallest max age time
        .min()
        // or fall back to the upper limit if no resources were retrieved
        .orElse(upperLimit);

    return Math.min(inputMaxAge, upperLimit);
  }

  List<TimeMeasurement> getSortedInputMaxAgeSeconds() {
    return TimeMeasurement.LONGEST_TIME_FIRST.sortedCopy(inputMaxAgeSeconds);
  }

  @Override
  public HalResource createMetadataResource(LinkableResource resourceImpl) {

    metadataWasRendered.set(true);

    return null;
  }

  @Override
  public void onMethodInvocationFinished(Class category, String methodDescription, long invocationDurationMicros) {
    // ignore all measurements
  }

  @Override
  public RequestMetricsStopwatch startStopwatch(Class measuringClass, Supplier<String> taskDescription) {
    return new RequestMetricsStopwatch() {

      @Override
      public void close() {
        // ignore all measurements
      }
    };
  }
}
