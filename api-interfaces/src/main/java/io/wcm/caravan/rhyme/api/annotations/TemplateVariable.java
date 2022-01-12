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

/**
 * Annotation to be used on parameters of methods annotated with {@link Related} to define
 * the name of the variable in the URI template that corresponds to the given parameter value.
 * <p>
 * If the template variable should have the same name as the parameter in the java method, then
 * you can omit this annotation, but you have to ensure that your parameter names are not stripped
 * out during compilation.
 * </p>
 * <p>
 * If you have link templates with many variables, consider using {@link TemplateVariables} to simplify
 * your method signatures.
 * </p>
 * @see TemplateVariables
 * @see Related
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface TemplateVariable {

  /**
   * @return the name of the template variable that corresponds to this method parameter
   */
  String value();
}
