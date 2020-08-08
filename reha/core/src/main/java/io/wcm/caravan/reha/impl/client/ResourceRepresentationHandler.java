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
package io.wcm.caravan.reha.impl.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.reha.api.annotations.ResourceRepresentation;
import io.wcm.caravan.reha.api.client.HalApiDeveloperException;

class ResourceRepresentationHandler {

  private final HalResource resource;

  ResourceRepresentationHandler(HalResource resource) {
    this.resource = resource;
  }

  Single<Object> handleMethodInvocation(HalApiMethodInvocation invocation) {

    Class<?> emissionType = invocation.getEmissionType();

    if (emissionType.isAssignableFrom(HalResource.class)) {
      return Single.just(resource);
    }

    if (emissionType.isAssignableFrom(ObjectNode.class)) {
      return Single.just(resource.getModel());
    }

    if (emissionType.isAssignableFrom(String.class)) {
      return Single.just(resource.getModel().toString());
    }

    throw new HalApiDeveloperException(
        "The method " + invocation + " annotated with @" + ResourceRepresentation.class.getSimpleName() + " must return "
            + "a reactive type emitting either " + HalResource.class.getSimpleName() + ", " + JsonNode.class.getSimpleName()
            + ", " + String.class.getSimpleName());
  }
}
