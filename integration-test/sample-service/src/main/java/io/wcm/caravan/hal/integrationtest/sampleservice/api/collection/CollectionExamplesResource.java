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

import io.reactivex.Single;
import io.wcm.caravan.hal.api.annotations.HalApiInterface;
import io.wcm.caravan.hal.api.annotations.RelatedResource;
import io.wcm.caravan.hal.api.annotations.ResourceState;
import io.wcm.caravan.hal.api.annotations.StandardRelations;
import io.wcm.caravan.hal.api.annotations.TemplateVariable;
import io.wcm.caravan.hal.api.annotations.TemplateVariables;
import io.wcm.caravan.hal.integrationtest.sampleservice.api.ExamplesEntryPointResource;

@HalApiInterface
public interface CollectionExamplesResource {

  @ResourceState
  Single<TitledState> getState();

  @RelatedResource(relation = StandardRelations.COLLECTION)
  Single<ItemCollectionResource> getCollection(
      @TemplateVariables CollectionParameters options);

  @RelatedResource(relation = StandardRelations.ITEM)
  Single<ItemResource> getItem(
      @TemplateVariable("index") Integer index,
      @TemplateVariable("delayMs") Integer delayMs);

  @RelatedResource(relation = "client:collection")
  Single<ItemCollectionResource> getCollectionThroughClient(
      @TemplateVariables CollectionParameters options);

  @RelatedResource(relation = "client:item")
  Single<ItemResource> getItemThroughClient(
      @TemplateVariable("index") Integer index,
      @TemplateVariable("delayMs") Integer delayMs);

  @RelatedResource(relation = StandardRelations.INDEX)
  Single<ExamplesEntryPointResource> getEntryPoint();
}
