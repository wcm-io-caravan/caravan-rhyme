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
 * Used to annotate methods that allow access to linked or embedded resources. The return value of methods
 * annotated with {@link Related} must provide one ore more instances of other {@link HalApiInterface} objects.
 * Methods with this annotation can have parameters with {@link TemplateVariable} or
 * {@link TemplateVariables} annotations to allow clients to expand link templates with the values specified
 * in those parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Related {

  /**
   * Defines the relation of the target resource to this context resource.
   * @return a standard relation or CURI of a custom relation (i.e. "prefix:relation")
   */
  String value();

}
