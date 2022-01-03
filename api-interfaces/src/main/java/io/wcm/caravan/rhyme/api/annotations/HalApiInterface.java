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

import io.wcm.caravan.rhyme.api.resources.EmbeddableResource;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * A marker annotation that is required for all HAL API interfaces to be used with the Rhyme framework. It indicates
 * that the interface can be scanned for the presence of methods annotated with {@link Related}, {@link ResourceState},
 * etc that actually define the API.
 * <p>
 * Note that you <b>can</b> make your interface extend {@link LinkableResource}, if the clients of the API should be
 * aware
 * that (and how) the resource can always be accessed through an URI.
 * </p>
 * <p>
 * If you are also using Rhyme for server-side rendering of the resource, then all your implementations of an interface
 * annotated with {@link HalApiInterface} <b>must</b> implement either {@link LinkableResource} or
 * {@link EmbeddableResource}.
 * </p>
 * @see Related
 * @see ResourceState
 * @see ResourceProperty
 * @see ResourceLink
 * @see ResourceRepresentation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HalApiInterface {

  /**
   * Allows to define a custom content type that is used when rendering server-side resource instances implementing
   * this interface
   * @return the custom content type (or default value of "application/hal+json")
   */
  String contentType() default "application/hal+json";
}
