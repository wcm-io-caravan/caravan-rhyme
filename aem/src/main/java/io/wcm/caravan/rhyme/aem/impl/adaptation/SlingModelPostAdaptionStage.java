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
package io.wcm.caravan.rhyme.aem.impl.adaptation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.sling.api.resource.Resource;

import io.wcm.caravan.rhyme.aem.api.adaptation.PostAdaptionStage;
import io.wcm.caravan.rhyme.aem.api.resources.SlingLinkableResource;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;

final class SlingModelPostAdaptionStage<I, M extends I> implements PostAdaptionStage<I, M> {

  private final SlingResourceAdapterImpl adapterImpl;

  private final Class<I> interfaze;
  private final Class<M> clazz;
  private final List<Consumer<M>> instanceDecorators;

  SlingModelPostAdaptionStage(SlingResourceAdapterImpl adapterImpl, Class<I> interfaze, Class<M> clazz) {
    this.adapterImpl = adapterImpl;
    this.interfaze = interfaze;
    this.clazz = clazz;
    this.instanceDecorators = new ArrayList<>();
  }

  private SlingModelPostAdaptionStage(SlingModelPostAdaptionStage<I, M> instance, List<Consumer<M>> instanceDecorators) {
    this.adapterImpl = instance.adapterImpl;
    this.interfaze = instance.interfaze;
    this.clazz = instance.clazz;
    this.instanceDecorators = instanceDecorators;
  }

  private SlingModelPostAdaptionStage<I, M> withInstanceDecorator(Consumer<M> decorator) {

    List<Consumer<M>> list = new ArrayList<>(instanceDecorators);
    list.add(decorator);

    return new SlingModelPostAdaptionStage<>(this, list);
  }

  private SlingModelPostAdaptionStage<I, M> withLinkDecorator(Consumer<SlingLinkableResource> decorator) {

    return withInstanceDecorator(instance -> {
      if (!(instance instanceof SlingLinkableResource)) {
        throw new HalApiDeveloperException(
            "Your model class " + instance.getClass().getSimpleName() + " does not implement " + SlingLinkableResource.class.getName()
                + " (which is required if you want to override link names and titles via SlingResourceAdapter)");
      }

      decorator.accept((SlingLinkableResource)instance);

    });
  }

  @Override
  public PostAdaptionStage<I, M> withModifications(Consumer<M> consumer) {
    return withInstanceDecorator(consumer);
  }

  @Override
  public PostAdaptionStage<I, M> withLinkTitle(String title) {

    return withLinkDecorator(r -> r.setLinkTitle(title));
  }

  @Override
  public PostAdaptionStage<I, M> withLinkName(String name) {

    return withLinkDecorator(r -> r.setLinkName(name));
  }

  @Override
  public PostAdaptionStage<I, M> withQueryParameters(Map<String, Object> parameters) {

    return withLinkDecorator(r -> r.setQueryParameters(parameters));
  }

  @Override
  public PostAdaptionStage<I, M> withPartialLinkTemplate() {

    return withLinkDecorator(r -> r.setExpandAllVariables(false));
  }

  @Override
  public PostAdaptionStage<I, M> withQueryParameterTemplate(String... names) {
    throw new HalApiDeveloperException("#withQueryParameterTemplatecan can only be called if you selected a null resource path to create a template");
  }

  @Override
  public M getInstance() {

    return getStreamOfModelType()
        .findFirst()
        .orElseThrow(() -> new HalApiDeveloperException("No resources were found after selecting " + adapterImpl.getSelectedResourcesDescription()));
  }

  @Override
  public Optional<I> getOptional() {

    return getStream().findFirst();
  }

  @Override
  public Stream<I> getStream() {

    return getResources().map(this::adaptToModelTypeAndDecorateInstances);
  }

  private Stream<M> getStreamOfModelType() {

    return getResources().map(this::adaptToModelTypeAndDecorateInstances);
  }

  private Stream<Resource> getResources() {
    Stream<Resource> resources = adapterImpl.getSelectedResources();

    if (resources == null) {
      throw new HalApiDeveloperException("No resources have been selected with this adapter");
    }

    Predicate<Resource> predicate = adapterImpl.getResourceFilterPredicate();
    if (predicate != null) {
      resources = resources.filter(predicate);
    }
    return resources;
  }

  private M adaptToModelTypeAndDecorateInstances(Resource res) {

    M model = adapterImpl.getSlingRhyme().adaptResource(res, this.clazz);

    instanceDecorators.forEach(decorator -> decorator.accept(model));

    return model;
  }

}
