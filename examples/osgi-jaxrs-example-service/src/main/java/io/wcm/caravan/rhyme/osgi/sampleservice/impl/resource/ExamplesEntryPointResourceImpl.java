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
package io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource;

import org.apache.commons.lang3.StringUtils;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.exceptions.HalApiDeveloperException;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.ExamplesEntryPointResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.caching.CachingExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.collection.CollectionExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.api.errors.ErrorExamplesResource;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.context.ExampleServiceRequestContext;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.caching.CachingExamplesResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.collection.CollectionExamplesResourceImpl;
import io.wcm.caravan.rhyme.osgi.sampleservice.impl.resource.errors.ErrorsExamplesResourceImpl;

public class ExamplesEntryPointResourceImpl implements ExamplesEntryPointResource, LinkableResource {

  private final ExampleServiceRequestContext context;

  private final boolean useFingerprintInSelfLink;

  public ExamplesEntryPointResourceImpl(ExampleServiceRequestContext context, boolean useFingerprintInSelfLink) {

    this.context = context;

    this.useFingerprintInSelfLink = useFingerprintInSelfLink;
  }

  @Override
  public Single<CollectionExamplesResource> getCollectionExamples() {

    return Single.just(new CollectionExamplesResourceImpl(context));
  }

  @Override
  public Single<CachingExamplesResource> getCachingExamples() {

    return Single.just(new CachingExamplesResourceImpl(context));
  }

  @Override
  public Single<ErrorExamplesResource> getErrorExamples() {

    return Single.just(new ErrorsExamplesResourceImpl(context));
  }

  @Override
  public HalResource asHalResource() {

    throw new HalApiDeveloperException("This method isn't implemented server-side, and only available through the Rhyme HTTP clients");
  }

  @Override
  public Link createLink() {

    Link link = context.buildLinkTo((resource, uriInfo, response) -> resource.getEntryPoint(uriInfo, response));

    String title = "The HAL API entry point of the OSGi/JAX-RS example service";

    if (!useFingerprintInSelfLink) {
      link.setHref(StringUtils.substringBefore(link.getHref(), "?"));
      title += " (current version)";
    }
    else {
      title += " (immutable for bundle version " + context.getBundleVersion() + ")";
    }

    return link.setTitle(title);
  }

  @Override
  public Maybe<ExamplesEntryPointResource> getLatestVersion() {

    if (context.hasFingerPrintedUrl()) {
      return Maybe.just(new ExamplesEntryPointResourceImpl(context, false));
    }

    return Maybe.empty();
  }

  @Override
  public Maybe<ExamplesEntryPointResource> getPermalink() {

    if (!context.hasFingerPrintedUrl()) {
      return Maybe.just(new ExamplesEntryPointResourceImpl(context, true));
    }

    return Maybe.empty();
  }

}
