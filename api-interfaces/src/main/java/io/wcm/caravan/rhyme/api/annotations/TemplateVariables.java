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
 * a single DTO parameter that is used to expand multiple template variables.
 * <p>
 * This can be used to simply method signatures with a lot of parameters, and making it easier to extend additional
 * variables later without breaking your method signatures.
 * </p>
 * <p>
 * To use this annotation you also need to define a single composite DTO type
 * with multiple fields that are named exactly as the corresponding template variables. DTO types to be used with
 * this annotation must either be a class with only public fields, or an interface with public getters. If you are using
 * interfaces then the variable names will be derived using Java bean conventions.
 * </p>
 * @see TemplateVariable
 * @see Related
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface TemplateVariables {
  // this is just a marker interface that does not have any configurable options
}
