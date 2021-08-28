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
package io.wcm.caravan.rhyme.api.common;

import java.util.function.Supplier;

import org.osgi.annotation.versioning.ProviderType;

import io.wcm.caravan.rhyme.api.Rhyme;

/**
 * Represents an ongoing measurement of execution time that was started with
 * {@link Rhyme#startStopwatch(Class, Supplier)} or {@link RequestMetricsCollector#startStopwatch(Class, Supplier)},
 * and needs to beclosed to finish the measurement.
 * This interface extends {@link AutoCloseable} so the easiest way to use this is to wrap the code block in a
 * try-with-resources statement
 */
@ProviderType
public interface RequestMetricsStopwatch extends AutoCloseable {

  /**
   * Stops the execution time measurement, and forward the result to the {@link RequestMetricsCollector} instance from
   * which this stopwatch was created.
   * It overrides {@link AutoCloseable#close()} because it doesn't throw any checked exceptions
   */
  @Override
  void close();

}
