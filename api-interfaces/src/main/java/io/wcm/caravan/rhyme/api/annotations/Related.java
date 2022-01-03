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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * Used to annotate methods in a {@link HalApiInterface} which allow access to linked or embedded resources.
 * <p>
 * The return value of the methods must provide one ore more instances of another {@link HalApiInterface} type.
 * These can either be returned directly, or wrapped in an {@link Optional}, {@link Stream}, {@link List},
 * or one of the supported reactive types (e.g. Single, Maybe, Observable).
 * </p>
 * <p>
 * You can also represent links to other non-HAL resources (e.g. HTML, text or binary) by using {@link LinkableResource}
 * as return type.
 * </p>
 * <p>
 * Methods with this annotation can have parameters with {@link TemplateVariable} or
 * {@link TemplateVariables} annotations to define URI templates that clients can expand with the values
 * specified in those parameters.
 * </p>
 * @see TemplateVariables
 * @see TemplateVariable
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Related {

  /**
   * Defines the relation of the linked or embedded resource to this context resource.
   * @return a standard relation or CURI of a custom relation (i.e. "prefix:relation")
   */
  String value();

}
