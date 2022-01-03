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

/**
 * Used to define methods that allow to access a single JSON property of a HAL resource's state.
 * <p>
 * The method must provide a value that can be easily transformed from/to JSON. This value can either be returned
 * directly from the annotated method, but can also be wrapped with {@link Optional} or a supported reactive type.
 * </p>
 * <p>
 * If you do have existing domain model classes in your project that can be easily (de)serialized, or
 * you feel that you are adding to many {@link ResourceProperty} methods to your resource interface,
 * then consider using {@link ResourceState} instead.
 * </p>
 * @see ResourceState
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResourceProperty {

  /**
   * Allows to override the name of the JSON property (by default it's derived from the method name)
   * @return the name of the JSON property (or empty string if not overriden)
   */
  String value() default "";
}
