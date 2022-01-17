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
package io.wcm.caravan.rhyme.impl.client.proxy;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.rhyme.api.annotations.ResourceRepresentation;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

class ResourceRepresentationHandler implements Function<HalResource, Observable<Object>> {

  private final HalApiMethodInvocation invocation;

  ResourceRepresentationHandler(HalApiMethodInvocation invocation) {
    this.invocation = invocation;
  }

  @Override
  public Observable<Object> apply(HalResource resource) {

    Class<?> emissionType = invocation.getEmissionType();

    if (emissionType.isAssignableFrom(HalResource.class)) {
      return Observable.just(resource);
    }

    if (emissionType.isAssignableFrom(ObjectNode.class)) {
      return Observable.just(resource.getModel());
    }

    if (emissionType.isAssignableFrom(String.class)) {
      return Observable.just(resource.getModel().toString());
    }

    throw new HalApiDeveloperException(
        "The method " + invocation + " annotated with @" + ResourceRepresentation.class.getSimpleName() + " must return "
            + "a reactive type emitting either " + HalResource.class.getSimpleName() + ", " + JsonNode.class.getSimpleName()
            + ", " + String.class.getSimpleName());
  }
}
