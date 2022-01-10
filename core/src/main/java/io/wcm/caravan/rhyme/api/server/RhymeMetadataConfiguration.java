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
package io.wcm.caravan.rhyme.api.server;

import io.wcm.caravan.rhyme.api.Rhyme;
import io.wcm.caravan.rhyme.api.RhymeBuilder;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * Defines configuration methods to influence if and how {@link Rhyme#renderResponse(LinkableResource)}
 * adds additional metadata to the response
 * @see RhymeBuilder#withMetadataConfiguration(RhymeMetadataConfiguration)
 * @see RequestMetricsCollector
 */
public interface RhymeMetadataConfiguration {

  /**
   * Determines whether an "rhyme:metadata" resource is automatically embedded into the response rendered with
   * {@link Rhyme#renderResponse(LinkableResource)}
   * @return true if metadata should be generated and enabled
   * @see RequestMetricsCollector
   */
  default boolean isMetadataGenerationEnabled() {
    return false;
  }
}
