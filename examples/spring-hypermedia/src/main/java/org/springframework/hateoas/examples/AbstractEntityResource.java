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
package org.springframework.hateoas.examples;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.data.util.Lazy;

import io.wcm.caravan.rhyme.api.exceptions.HalApiServerException;
import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

abstract class AbstractEntityResource<T> implements LinkableResource, EmbeddableResource {

  protected final Long id;

  private final Lazy<T> state;

  private final boolean embedded;

  AbstractEntityResource(Long id, Supplier<Optional<T>> supplier) {
    this.id = id;
    this.state = Lazy.of(() -> supplier.get()
        .orElseThrow(() -> new HalApiServerException(404, "No entity was found with id " + id)));
    this.embedded = false;
  }

  AbstractEntityResource(Long id, T entity) {
    this.id = id;
    this.state = Lazy.of(entity);
    this.embedded = true;
  }

  public T getState() {
    return state.get();
  }

  @Override
  public boolean isEmbedded() {
    return embedded;
  }
}
