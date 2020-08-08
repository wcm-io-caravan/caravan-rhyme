/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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
package io.wcm.caravan.reha.impl.renderer;

import org.apache.commons.lang3.RandomUtils;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.reha.api.common.RequestMetricsCollector;
import io.wcm.caravan.reha.api.resources.LinkableResource;
import io.wcm.caravan.reha.impl.metadata.ResponseMetadataGenerator;
import io.wcm.caravan.reha.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.reha.testing.TestState;


public final class AsyncHalResourceRendererTestUtil {

  private AsyncHalResourceRendererTestUtil() {
    // static methods only
  }

  public static HalResource render(Object resourceImplInstance) {

    RequestMetricsCollector metrics = new ResponseMetadataGenerator();
    AsyncHalResourceRendererImpl renderer = new AsyncHalResourceRendererImpl(metrics, new DefaultHalApiTypeSupport());

    Single<HalResource> rxResource;
    if (resourceImplInstance instanceof LinkableResource) {
      rxResource = renderer.renderResource((LinkableResource)resourceImplInstance);
    }
    else {
      rxResource = renderer.renderLinkedOrEmbeddedResource(resourceImplInstance);
    }

    return rxResource.toObservable().blockingFirst();
  }

  static Single<LinkableResource> createSingleExternalLinkedResource(Link link) {
    return Single.just(new LinkableResource() {

      @Override
      public Link createLink() {
        return link;
      }
    });
  }

  static Single<LinkableResource> createSingleExternalLinkedResource(String uri) {
    return createSingleExternalLinkedResource(new Link(uri));
  }

  public static TestState createTestState() {
    return new TestState("This is just a test", RandomUtils.nextInt(0, 100000));
  }

}
