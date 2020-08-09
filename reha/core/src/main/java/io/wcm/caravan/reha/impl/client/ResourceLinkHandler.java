/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
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
package io.wcm.caravan.reha.impl.client;

import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.annotations.ResourceLink;
import io.wcm.caravan.reha.api.exceptions.HalApiDeveloperException;

class ResourceLinkHandler {

  private final Link link;

  ResourceLinkHandler(Link link) {
    this.link = link;
  }

  Object handleMethodInvocation(HalApiMethodInvocation invocation) {
    Class<?> returnType = invocation.getReturnType();

    if (returnType.isAssignableFrom(Link.class)) {
      if (link == null) {
        return new Link("");
      }
      return link;
    }

    if (returnType.isAssignableFrom(String.class)) {
      if (link == null) {
        return "";
      }
      return link.getHref();
    }

    throw new HalApiDeveloperException(
        "the method " + invocation + " annotated with @" + ResourceLink.class.getSimpleName() + " must return either a String or " + Link.class.getName());
  }
}
