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
package io.wcm.caravan.rhyme.aem.impl.util;

import java.util.Optional;

import org.apache.sling.api.resource.Resource;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

public final class PageUtils {

  private PageUtils() {
    // this class only contains static methods
  }

  public static Resource getPageResource(Resource resource) {

    return getOptionalPageResource(resource)
        .orElseThrow(() -> new HalApiDeveloperException("The resource " + resource + " is not a page, and not located within a page"));
  }

  public static Optional<Resource> getOptionalPageResource(Resource resource) {

    Resource candidate = resource;
    while (candidate != null && !PageUtils.isPage(candidate)) {
      candidate = candidate.getParent();
    }

    return Optional.ofNullable(candidate);
  }

  public static Resource getParentPageResource(Resource resource) {

    Resource page = getPageResource(resource);

    Resource parent = page.getParent();

    if (!PageUtils.isPage(parent)) {
      throw new HalApiDeveloperException("The parent resource " + parent + " is not a page");
    }
    return parent;
  }

  public static Resource getGrandParentPageResource(Resource resource) {

    Resource parentPage = getParentPageResource(resource);

    return getParentPageResource(parentPage);
  }

  public static boolean isPage(Resource page) {

    return "cq:Page".equals(page.getResourceType());
  }
}
