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
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionParameters;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemCollectionResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.ItemResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.TitledState;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;

public class CollectionExamplesResourceImpl implements CollectionExamplesResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  public CollectionExamplesResourceImpl(ExampleServiceRequestContext context) {
    this.context = context;
  }

  @Override
  public Single<TitledState> getState() {

    return Single.just(new TitledState());
  }

  @Override
  public Single<ItemCollectionResource> getCollection(CollectionParameters params) {

    return Single.just(new DelayableCollectionResourceImpl(context, null));
  }

  @Override
  public Single<ItemResource> getItem(Integer index, Integer delayMs) {

    return Single.just(new DelayableItemResourceImpl(context, index, delayMs));
  }

  @Override
  public Single<ItemCollectionResource> getCollectionThroughClient(CollectionParameters params) {

    return Single.just(new ClientCollectionResourceImpl(context, null));
  }

  @Override
  public Single<ItemResource> getItemThroughClient(Integer index, Integer delayMs) {

    return Single.just(new ClientItemResourceImpl(context, index, delayMs));
  }

  @Override
  public Single<ExamplesEntryPointResource> getEntryPoint() {

    return Single.just(new ExamplesEntryPointResourceImpl(context));
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo((resource, uriInfo, response) -> resource.getCollectionExamples(uriInfo, response))
        .setTitle("Examples for handling collections of linked or embedded resources");
  }
}
