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
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Used to declare the method that allows to access the HAL resources' state (all JSON properties except for
 * "_links" and "_embedded") as a single object.
 * <p>
 * The method must provide either a Jackson {@link ObjectNode} or a Java object that that matches the JSON object
 * structure (and can be deserialized by a default Jackson {@link ObjectMapper}). This object can either be returned
 * directly from the annotated method, but can also be wrapped with {@link Optional} or a supported reactive type.
 * </p>
 * @see ResourceProperty
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResourceState {
  // this is just a marker interface that does not have any configurable options
}
