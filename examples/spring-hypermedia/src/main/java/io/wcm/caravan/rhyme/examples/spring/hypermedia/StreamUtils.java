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
package io.wcm.caravan.rhyme.examples.spring.hypermedia;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

final class StreamUtils {

  private StreamUtils() {
    // this class contains only static utility methods
  }

  /**
   * Creates a resource implementation instance for each of the given entities.
   * @param <T> entity type
   * @param <U> resource implementation type
   * @param entities loaded from the repository
   * @param resourceConstructor creates a resource implementation for a given entity
   * @return a list with one resource for each entity
   */
  static <T, U> List<U> createResourcesFrom(Iterable<T> entities, Function<T, U> resourceConstructor) {

    return StreamSupport.stream(entities.spliterator(), false)
        .map(resourceConstructor)
        .collect(Collectors.toList());
  }
}
