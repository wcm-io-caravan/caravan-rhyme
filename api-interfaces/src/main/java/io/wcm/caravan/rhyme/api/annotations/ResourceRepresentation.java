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
package io.wcm.caravan.rhyme.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.databind.JsonNode;

import io.wcm.caravan.hal.resource.HalResource;

/**
 * Used to declare the method that allows clients to get the full JSON representation of the resource
 * (including the _links and _embedded properties).
 * <p>
 * Adding a method with this annotation is only required if you want your clients to conveniently process the HAL
 * resources manually (or with another HAL library). It's not required if you are working exclusively with Rhyme.
 * </p>
 * <p>
 * The method must return a {@link JsonNode} or {@link HalResource} (either directly or wrapped in a supported reactive
 * type).
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResourceRepresentation {
  // this is just a marker interface that does not have any configurable options
}
