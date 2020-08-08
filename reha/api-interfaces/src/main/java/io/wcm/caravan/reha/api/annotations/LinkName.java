/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.reha.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that can be used for parameters of methods annotated with {@link RelatedResource} to indicate
 * that a HAL API client should only consider link(s) with a specific name attribute.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface LinkName {

  // TODO: decide if this annotation is really required. The same kind of filtering can also be achieved by
  // adding a @ResourceLink method on the linked resource and then filtering links by name before following them.
  // That requires a bit more complicated code, but this is a rare use cases, and the downside of having this
  // annotation is that it is relevant for the client-side only (you would never *implement* methods using it on the server side)
}
