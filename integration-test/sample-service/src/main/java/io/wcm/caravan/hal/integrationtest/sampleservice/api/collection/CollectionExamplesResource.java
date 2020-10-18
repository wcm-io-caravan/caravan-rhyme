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
package io.wcm.caravan.hal.integrationtest.sampleservice.api.collection;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.reha.api.annotations.HalApiInterface;
import io.wcm.caravan.reha.api.annotations.Related;
import io.wcm.caravan.reha.api.annotations.ResourceState;
import io.wcm.caravan.reha.api.annotations.TemplateVariable;
import io.wcm.caravan.reha.api.annotations.TemplateVariables;
import io.wcm.caravan.reha.api.relations.StandardRelations;

@HalApiInterface
public interface CollectionExamplesResource {

  @ResourceState
  Single<TitledState> getState();

  @Related(StandardRelations.COLLECTION)
  Single<ItemCollectionResource> getCollection(
      @TemplateVariables CollectionParameters options);

  @Related(StandardRelations.ITEM)
  Single<ItemResource> getItem(
      @TemplateVariable("index") Integer index,
      @TemplateVariable("delayMs") Integer delayMs);

  @Related("client:collection")
  Single<ItemCollectionResource> getCollectionThroughClient(
      @TemplateVariables CollectionParameters options);

  @Related("client:item")
  Single<ItemResource> getItemThroughClient(
      @TemplateVariable("index") Integer index,
      @TemplateVariable("delayMs") Integer delayMs);

  @Related(StandardRelations.INDEX)
  Single<ExamplesEntryPointResource> getEntryPoint();
}
