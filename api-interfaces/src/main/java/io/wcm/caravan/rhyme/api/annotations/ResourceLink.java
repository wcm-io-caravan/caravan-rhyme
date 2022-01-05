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

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

/**
 * Used to declare a method that provide a link to the resource instance. The return type of the method must be a
 * {@link Link} or {@link String}.
 * <p>
 * On the client side, it will allow to access only the {@link Link} or URI that is pointing to a resource
 * (without actually fetching it). On the server side, this annotation is usually used only indirectly
 * by implementing {@link LinkableResource#createLink()}.
 * <p>
 * If your interface extends from {@link LinkableResource} then you won't need to add this annotation yourself, as
 * {@link LinkableResource#createLink()} is already annotated.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResourceLink {
  // this is just a marker interface that does not have any configurable options
}
