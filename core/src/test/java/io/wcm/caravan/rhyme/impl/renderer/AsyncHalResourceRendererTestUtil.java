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
package io.wcm.caravan.rhyme.impl.renderer;

import org.apache.commons.lang3.RandomUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.reactivex.rxjava3.core.Single;
import io.wcm.caravan.hal.resource.HalResource;
import io.wcm.caravan.hal.resource.Link;
import io.wcm.caravan.rhyme.api.common.RequestMetricsCollector;
import io.wcm.caravan.rhyme.api.resources.LinkableResource;
import io.wcm.caravan.rhyme.impl.metadata.FullMetadataGenerator;
import io.wcm.caravan.rhyme.impl.reflection.DefaultHalApiTypeSupport;
import io.wcm.caravan.rhyme.testing.TestState;


public final class AsyncHalResourceRendererTestUtil {

  private AsyncHalResourceRendererTestUtil() {
    // static methods only
  }

  public static HalResource render(Object resourceImplInstance) {

    RequestMetricsCollector metrics = new FullMetadataGenerator();
    AsyncHalResourceRendererImpl renderer = new AsyncHalResourceRendererImpl(metrics, new DefaultHalApiTypeSupport(), new ObjectMapper());

    Single<HalResource> rxResource;
    if (resourceImplInstance instanceof LinkableResource) {
      rxResource = renderer.renderResource((LinkableResource)resourceImplInstance);
    }
    else {
      rxResource = renderer.renderResourceAndEmbedded(resourceImplInstance);
    }

    return rxResource.toObservable().blockingFirst();
  }

  static Single<Link> createSingleExternalLinkedResource(Link link) {
    return Single.just(link);
  }

  static Single<Link> createSingleExternalLinkedResource(String uri) {
    return createSingleExternalLinkedResource(new Link(uri));
  }

  public static TestState createTestState() {
    return new TestState("This is just a test", RandomUtils.nextInt(0, 100000));
  }

}
