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
 * annotated with {@link RelatedResource} must be a reactive stream of another interface annotated with
 * {@link HalApiInterface}. Methods with this annotation can have parameters with {@link TemplateVariable} or
 * {@link TemplateVariables} annotations to allow clients to expand link templates with the values specified
 * in those parameters.
 */
// TODO: this could be renamed to @Related which is shorter and doesn't imply that it returns just a single resource
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RelatedResource {

  /**
   * Defines the relation of the target resource to this context resource.
   * @return a standard relation or CURI of a custom relation (i.e. "prefix:relation")
   */
  String relation();

  /**
   * Defines the HTTP method that should be used by clients when following links with this relation
   * @return "GET" or any other HTTP method explicitly defined in the annotation
   */
  // TODO: this parameter should be removed as long as we don't actually support anything but "GET"
  // another benefit of removing this parameter is that if we only have a single parameter
  // we could just use "value" so using the annotation would be as simple as @RelatedResource("relation")
  String method() default "GET";

}
