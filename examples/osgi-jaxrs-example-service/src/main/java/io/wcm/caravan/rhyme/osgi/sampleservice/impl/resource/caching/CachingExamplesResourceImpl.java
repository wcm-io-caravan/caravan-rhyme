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
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.caching;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.caching.CachingExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.caching.EvenOddItemsResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionParameters;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.ExamplesEntryPointResourceImpl;

public class CachingExamplesResourceImpl implements CachingExamplesResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  public CachingExamplesResourceImpl(ExampleServiceRequestContext context) {
    this.context = context;
  }

  @Override
  public Single<EvenOddItemsResource> getEvenAndOddItems(CollectionParameters parameters) {

    return Single.just(new EvenAndOddItemsResourceImpl(context, null));
  }

  @Override
  public Single<ExamplesEntryPointResource> getEntryPoint() {

    return Single.just(new ExamplesEntryPointResourceImpl(context));
  }

  @Override
  public Link createLink() {

    return context.buildLinkTo((resource, uriInfo, response) -> resource.getCachingExamples(uriInfo, response))
        .setTitle("Examples for resources that need local caching to avoid multiple identical requests to upstream server");
  }
}
