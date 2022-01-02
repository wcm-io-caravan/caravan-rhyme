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

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
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

  Observable<Object> handleMethodInvocation(HalApiMethodInvocation invocation) {

    String propertyName = HalApiReflectionUtils.getPropertyName(invocation.getMethod(), typeSupport);

    JsonNode jsonNode = contextResource.getModel().path(propertyName);

    if (jsonNode.isMissingNode() || jsonNode.isNull()) {

      if (!typeSupport.isProviderOfOptionalValue(invocation.getReturnType())) {

        String msg = "The JSON property '" + propertyName + "' is " + jsonNode.getNodeType()
            + ". You must use Maybe or Optional as return type to support this. ";

        return errorObservable(msg);
      }
      return Observable.empty();
    }

    if (typeSupport.isProviderOfMultiplerValues(invocation.getReturnType())) {
      if (!jsonNode.isArray()) {
        return errorObservable("The JSON property '" + propertyName + "' is of type " + jsonNode.getNodeType()
            + " but an array was expected. Please adjust " + invocation + " accordingly");
      }

      return Observable.fromIterable(jsonNode)
          .map(arrayElement -> convertToJavaObject(invocation, arrayElement));
    }

    if (jsonNode.isArray()) {
      return errorObservable("The JSON property '" + propertyName + "' is an array, but a primitive or object "
          + "(" + invocation.getReturnType().getSimpleName() + ") was expected");
    }

    return Observable.just(convertToJavaObject(invocation, jsonNode));
  }

  Object convertToJavaObject(HalApiMethodInvocation invocation, JsonNode jsonNode) {
    Object propertyValue = OBJECT_MAPPER.convertValue(jsonNode, invocation.getEmissionType());
    return propertyValue;
  }

  Observable<Object> errorObservable(String msg) {

    String fullMsg = msg;

    Link link = contextResource.getLink();
    if (link != null) {
      fullMsg += " (The error was triggered by resource at " + link.getHref() + ")";
    }

    return Observable.error(new HalApiDeveloperException(fullMsg));
  }
}
