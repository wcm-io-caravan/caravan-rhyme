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
import com.fasterxml.jackson.databind.ObjectMapper;

import io.reactivex.rxjava3.core.Observable;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.impl.reflection.HalApiReflectionUtils;
import io.wcm.caravan.rhyme.impl.reflection.HalApiTypeSupport;

class ResourcePropertyHandler implements Function<HalResource, Observable<Object>> {

  private final HalApiMethodInvocation invocation;
  private final HalApiTypeSupport typeSupport;
  private final ObjectMapper objectMapper;

  ResourcePropertyHandler(HalApiMethodInvocation invocation, HalApiTypeSupport typeSupport, ObjectMapper objectMapper) {
    this.invocation = invocation;
    this.typeSupport = typeSupport;
    this.objectMapper = objectMapper;
  }

  @Override
  public Observable<Object> apply(HalResource contextResource) {

    String propertyName = HalApiReflectionUtils.getPropertyName(invocation.getMethod(), typeSupport);

    JsonNode jsonNode = contextResource.getModel().path(propertyName);

    if (jsonNode.isMissingNode() || jsonNode.isNull()) {

      if (!typeSupport.isProviderOfOptionalValue(invocation.getReturnType())) {

        String msg = "The JSON property '" + propertyName + "' is " + jsonNode.getNodeType()
            + ". You must use Maybe or Optional as return type to support this. ";

        return errorObservable(msg, contextResource);
      }
      return Observable.empty();
    }

    if (typeSupport.isProviderOfMultiplerValues(invocation.getReturnType())) {
      if (!jsonNode.isArray()) {
        return errorObservable("The JSON property '" + propertyName + "' is of type " + jsonNode.getNodeType()
            + " but an array was expected. Please adjust " + invocation + " accordingly", contextResource);
      }

      return Observable.fromIterable(jsonNode)
          .map(arrayElement -> convertToJavaObject(invocation, arrayElement));
    }

    if (jsonNode.isArray()) {
      return errorObservable("The JSON property '" + propertyName + "' is an array, but a primitive or object "
          + "(" + invocation.getReturnType().getSimpleName() + ") was expected", contextResource);
    }

    return Observable.just(convertToJavaObject(invocation, jsonNode));
  }

  Object convertToJavaObject(HalApiMethodInvocation invocation, JsonNode jsonNode) {
    return objectMapper.convertValue(jsonNode, invocation.getEmissionType());
  }

  Observable<Object> errorObservable(String msg, HalResource contextResource) {

    String fullMsg = msg;

    Link link = contextResource.getLink();
    if (link != null) {
      fullMsg += " (The error was triggered by resource at " + link.getHref() + ")";
    }

    return Observable.error(new HalApiDeveloperException(fullMsg));
  }
}
