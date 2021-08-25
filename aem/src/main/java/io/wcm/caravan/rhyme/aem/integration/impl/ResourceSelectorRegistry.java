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
package io.wcm.caravan.rhyme.aem.integration.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.wcm.caravan.rhyme.aem.integration.ResourceSelectorProvider;
import io.wcm.caravan.rhyme.aem.integration.SlingResourceAdapter;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;

@Component(service = ResourceSelectorRegistry.class)
public class ResourceSelectorRegistry {

  @Reference(cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.STATIC,
      policyOption = ReferencePolicyOption.GREEDY)
  private List<ResourceSelectorProvider> providers;

  public Optional<Class<? extends LinkableResource>> getModelClassForSelectors(Collection<String> selectors) {

    List<Class<? extends LinkableResource>> customModelClasses = providers.stream()
        .flatMap(provider -> provider.getModelClassesWithSelectors().entrySet().stream())
        .filter(entry -> selectors.contains(entry.getValue()))
        .map(entry -> entry.getKey())
        .distinct()
        .collect(Collectors.toList());

    if (customModelClasses.isEmpty()) {
      return Optional.empty();
    }

    if (customModelClasses.size() > 1) {

      String classNames = customModelClasses.stream()
          .map(Class::getName)
          .collect(Collectors.joining(", "));

      String msg = "More than one resource was registered for selector [" + StringUtils.join(selectors, ",") + "]: " + classNames;

      throw new HalApiDeveloperException(msg);
    }

    return Optional.of(customModelClasses.iterator().next());
  }

  public Optional<String> getSelectorForModelClass(Class<?> clazz) {

    return providers.stream()
        .map(provider -> provider.getModelClassesWithSelectors().get(clazz))
        .filter(Objects::nonNull)
        .findFirst();
  }

  public Optional<String> getSelectorForHalApiInterface(Class<?> halApiInterface) {

    Optional<Class<? extends LinkableResource>> modelClass = providers.stream()
        .flatMap(provider -> provider.getModelClassesWithSelectors().keySet().stream())
        .filter(clazz -> halApiInterface.isAssignableFrom(clazz))
        .findFirst();

    return modelClass
        .flatMap(this::getSelectorForModelClass);
  }

  public Stream<LinkableResource> getAllApiEntryPoints(SlingResourceAdapter adapter) {

    return providers.stream()
        .map(provider -> provider.getApiEntryPoint(adapter))
        .flatMap(this::optionalAsStream);
  }

  private <T> Stream<? extends LinkableResource> optionalAsStream(Optional<? extends LinkableResource> optional) {

    return optional.isPresent() ? Stream.of(optional.get()) : Stream.empty();
  }

}
