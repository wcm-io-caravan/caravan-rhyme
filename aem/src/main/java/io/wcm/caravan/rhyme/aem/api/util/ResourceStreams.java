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
package io.wcm.caravan.rhyme.aem.api.util;

import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;

import com.day.cq.commons.jcr.JcrConstants;

import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

public final class ResourceStreams {

  private ResourceStreams() {
    // this class only contains static utility methods
  }

  public static Stream<Resource> getChildren(Resource res) {

    return StreamSupport.stream(res.getChildren().spliterator(), false);
  }

  public static Stream<Resource> getNamedChild(Resource res, String name) {

    return Stream.of(res.getChild(name))
        .filter(Objects::nonNull);
  }

  public static Stream<Resource> getChildPages(Resource res) {

    Resource pageResource = getPageResource(res);

    return getChildren(pageResource)
        .filter(ResourceStreams::isPage);
  }

  public static Stream<Resource> getContentOfContainingPage(Resource res) {

    Resource pageResource = getPageResource(res);

    return getContentResources(Stream.of(pageResource));
  }

  public static Stream<Resource> getContentOfChildPages(Resource res) {

    Resource pageResource = getPageResource(res);

    Stream<Resource> childPages = getChildPages(pageResource);

    return getContentResources(childPages);
  }

  public static Stream<Resource> getContentOfGrandChildPages(Resource res) {

    Resource pageResource = getPageResource(res);

    Stream<Resource> grandChildPages = getChildPages(pageResource)
        .flatMap(ResourceStreams::getChildPages);

    return getContentResources(grandChildPages);
  }

  public static Stream<Resource> getContentOfNamedChildPage(Resource res, String name) {

    Resource pageResource = getPageResource(res);

    Stream<Resource> childPage = ResourceStreams.getChildPages(pageResource)
        .filter(child -> name.equals(child.getName()));

    return getContentResources(childPage);
  }

  public static Stream<Resource> getParent(Resource res) {

    return Stream.of(res.getParent()).filter(Objects::nonNull);
  }

  public static Resource getPageResource(Resource resource) {
    Resource candidate = resource;
    while (candidate != null && !isPage(candidate)) {
      candidate = candidate.getParent();
    }
    if (candidate == null) {
      throw new HalApiDeveloperException("The resource " + resource + " is not a page, and not located within a page");
    }
    return candidate;
  }

  public static Resource getParentPageResource(Resource resource) {
    Resource page = getPageResource(resource);
    Resource parent = page.getParent();
    if (!isPage(parent)) {
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

  public static Stream<Resource> getContentResource(Resource pageResource) {

    return Stream.of(pageResource.getChild(JcrConstants.JCR_CONTENT))
        .filter(Objects::nonNull);
  }


  public static Stream<Resource> getContentResources(Stream<Resource> pageResources) {

    return pageResources
        .filter(Objects::nonNull)
        .flatMap(ResourceStreams::getContentResource);
  }

}
