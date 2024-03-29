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
package io.wcm.caravan.rhyme.impl.documentation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.annotations.HalApiInterface;
import io.wcm.caravan.rhyme.api.spi.RhymeDocsSupport;

/**
 * A class that generates links with the "curies" relation which point to the
 * location where the generated HTML documentation is served
 */
public class RhymeDocsCurieGenerator {

  private final String baseUrl;
  private final boolean includeFragment;

  /**
   * @param docsSupport documentation integration configuration
   */
  public RhymeDocsCurieGenerator(RhymeDocsSupport docsSupport) {
    this.baseUrl = docsSupport.getRhymeDocsBaseUrl();
    this.includeFragment = docsSupport.isFragmentAppendedToCuriesLink();
  }

  /**
   * Adds a documentation link with "curies" relation for all custom relations present in the given resource.
   * The link will point to the HTML documentation of the given {@link HalApiInterface}.
   * @param halResource where the links should be added
   * @param halApiInterface used to generate the file name of the documentation URL
   */
  public void addCuriesTo(HalResource halResource, Class<?> halApiInterface) {

    List<Link> curieLinks = createCurieLinks(halResource, halApiInterface);

    halResource.addLinks("curies", curieLinks);
  }

  private List<Link> createCurieLinks(HalResource halResource, Class<?> halApiInterface) {

    String fileName = halApiInterface.getName() + ".html";

    Set<String> curieNames = collectCurieNames(halResource);

    return curieNames.stream()
        .map(curieName -> createCurieLink(curieName, fileName))
        .collect(Collectors.toList());
  }

  private Set<String> collectCurieNames(HalResource hal) {

    return collectRelationsRecursively(hal)
        .distinct()
        .filter(rel -> rel.contains(":"))
        .map(rel -> StringUtils.substringBefore(rel, ":"))
        .collect(Collectors.toSet());
  }

  private Stream<String> collectRelationsRecursively(HalResource hal) {

    Stream<String> relationsInThisResource = Stream.concat(
        hal.getLinks().keys().stream(),
        hal.getEmbedded().keys().stream())
        .distinct();

    Stream<String> relationsInEmbeddedResources = hal.getEmbedded().values().stream()
        .flatMap(this::collectRelationsRecursively)
        .distinct();

    return Stream.concat(relationsInThisResource, relationsInEmbeddedResources);
  }

  private Link createCurieLink(String curieName, String fileName) {

    String prefix = baseUrl;
    if (!prefix.endsWith("/")) {
      prefix += "/";
    }

    String href = prefix + fileName;
    if (includeFragment) {
      href += "#" + curieName + ":{rel}";
    }

    return new Link(href)
        .setName(curieName)
        .setTitle("HTML documentation for relations with " + curieName + " prefix");
  }
}
