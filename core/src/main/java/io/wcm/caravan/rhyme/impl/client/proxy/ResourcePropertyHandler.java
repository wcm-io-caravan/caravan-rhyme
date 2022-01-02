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

import static io.wcm.caravan.rhyme.impl.client.proxy.ResourceStateHandler.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.rxjava3.core.Maybe;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.ResourceProperty;
import io.wcm.caravan.rhyme.api.annotations.ResourceState;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;

class ResourcePropertyHandler {

  private final HalResource contextResource;
  private final HalApiTypeSupport typeSupport;

  ResourcePropertyHandler(HalResource contextResource, HalApiTypeSupport typeSupport) {
    this.contextResource = contextResource;
    this.typeSupport = typeSupport;
  }

  Maybe<Object> handleMethodInvocation(HalApiMethodInvocation invocation) {

    if (typeSupport.isProviderOfMultiplerValues(invocation.getReturnType())) {
      String msg = "@" + ResourceProperty.class.getSimpleName() + " cannot be used for arrays, but " + invocation + " is using " + invocation.getReturnType()
          + " as return type. Consider using @" + ResourceState.class.getSimpleName() + " instead";
      return Maybe.error(new HalApiDeveloperException(msg));
    }

    String propertyName = HalApiReflectionUtils.getPropertyName(invocation.getMethod(), typeSupport);

    JsonNode jsonNode = contextResource.getModel().path(propertyName);

    if (jsonNode.isMissingNode() || jsonNode.isNull()) {

      if (!typeSupport.isProviderOfOptionalValue(invocation.getReturnType())) {

        String msg = "The JSON property '" + propertyName + "' is " + jsonNode.getNodeType()
            + ". You must use Maybe or Optional as return type to support this. ";

        Link link = contextResource.getLink();
        if (link != null) {
          msg += " (The error was triggered by resource at " + link.getHref() + ")";
        }

        return Maybe.error(new HalApiDeveloperException(msg));
      }
      return Maybe.empty();
    }

    Object propertyValue = OBJECT_MAPPER.convertValue(jsonNode, invocation.getEmissionType());

    return Maybe.just(propertyValue);
  }
}
