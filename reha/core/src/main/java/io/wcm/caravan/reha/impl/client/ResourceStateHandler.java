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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.rxjava3.core.Maybe;
import io.wcm.caravan.hal.resource.HalResource;

class ResourceStateHandler {

  private static final Logger log = LoggerFactory.getLogger(HalApiInvocationHandler.class);

  private final HalResource contextResource;

  ResourceStateHandler(HalResource contextResource) {
    this.contextResource = contextResource;
  }

  Maybe<Object> handleMethodInvocation(HalApiMethodInvocation invocation) {

    Class<?> returnType = invocation.getReturnType();

    log.trace(invocation + " was invoked, method is annotated with @ResourceState and returns type " + returnType.getSimpleName());

    // if the interface is using Maybe or Optional as return type, and the HAL resource does not contain any
    // state properties, then an empty maybe should be returned
    boolean isOptional = Maybe.class.isAssignableFrom(returnType) || Optional.class.isAssignableFrom(returnType);
    if (isOptional && contextResource.getStateFieldNames().isEmpty()) {
      return Maybe.empty();
    }

    // if it is an observable then we have to use the emission type as target of the conversion
    Object properties = convertResourceProperties(invocation.getEmissionType());

    return Maybe.just(properties);
  }

  private Object convertResourceProperties(Class<?> resourcePropertiesType) {

    return contextResource.adaptTo(resourcePropertiesType);
  }

}
